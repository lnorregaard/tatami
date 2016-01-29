package fr.ippon.tatami.service.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.Ping;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.Username;
import fr.ippon.tatami.domain.status.Status;
import fr.ippon.tatami.repository.FriendRepository;
import fr.ippon.tatami.repository.GroupRepository;
import fr.ippon.tatami.repository.UsernameRepository;
import fr.ippon.tatami.service.SearchService;
import fr.ippon.tatami.service.dto.UserFavouriteCountDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.HasChildQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

public class ElasticsearchSearchService implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSearchService.class);

    private static final String ALL_FIELD = "_all";

    private static final List<String> TYPES = Collections.unmodifiableList(Arrays.asList("user", "status", "group","firstname", "favourite"));

    @Inject
    private ElasticsearchEngine engine;

    @Inject
    private String indexNamePrefix;

    @Inject
    private GroupRepository groupRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private UsernameRepository usernameRepository;

    private Client client() {
        return engine.client();
    }

    private String indexName(String type) {
        return StringUtils.isEmpty(indexNamePrefix) ? type : indexNamePrefix + '-' + type;
    }

    @PostConstruct
    private void init() {
        for (String type : TYPES) {
            if (!client().admin().indices().prepareExists(indexName(type)).execute().actionGet().isExists()) {
                log.info("Index {} does not exists in Elasticsearch, creating it!", indexName(type));
                createIndex();
            }
        }
    }

    @Override
    public boolean reset() {
        log.info("Reseting ElasticSearch Index");
        if (deleteIndex()) {
            return createIndex();
        } else {
            log.warn("ElasticSearch Index could not be reset!");
            return false;
        }
    }

    /**
     * Delete the tatami index.
     *
     * @return {@code true} if the index is deleted or didn't exist.
     */
    private boolean deleteIndex() {
        for (String type : TYPES) {
            try {
                boolean ack = client().admin().indices().prepareDelete(indexName(type)).execute().actionGet().isAcknowledged();
                if (!ack) {
                    log.error("Elasticsearch Index wasn't deleted !");
                    return false;
                }
            } catch (Exception e) {
                log.error("Elasticsearch Index " + indexName(type) + " was not deleted", e);
                return false;

            }
//            } catch (IndexMissingException e) {
//                // Failling to delete a missing index is supposed to be valid
//                log.warn("Elasticsearch Index " + indexName(type) + " missing, it was not deleted");
//
//            } catch (ElasticSearchException e) {
//            }
        }
        log.debug("Elasticsearch Index deleted!");
        return true;
    }

    /**
     * Create the tatami index.
     *
     * @return {@code true} if an error occurs during the creation.
     */
    private boolean createIndex() {
        for (String type : TYPES) {
            if (!createSpecificIndex(type)) {
                return false;
            }
        }
        return true;
    }

    private boolean createSpecificIndex(String type) {
        try {
            CreateIndexRequestBuilder createIndex = client().admin().indices().prepareCreate(indexName(type));
            URL mappingUrl = getClass().getClassLoader().getResource("META-INF/elasticsearch/index/" + type + ".json");

            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode indexConfig = jsonMapper.readTree(mappingUrl);
            JsonNode indexSettings = indexConfig.get("settings");
            if (indexSettings != null && indexSettings.isObject()) {
                createIndex.setSettings(jsonMapper.writeValueAsString(indexSettings));
            }

            JsonNode mappings = indexConfig.get("mappings");
            if (mappings != null && mappings.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> i = mappings.fields(); i.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = i.next();
                    ObjectNode mapping = jsonMapper.createObjectNode();
                    mapping.put(field.getKey(), field.getValue());
                    createIndex.addMapping(field.getKey(), jsonMapper.writeValueAsString(mapping));
                }
            }

            boolean ack = createIndex.execute().actionGet().isAcknowledged();
            if (!ack) {
                log.error("Cannot create index " + indexName(type));
                return false;
            }

        } catch (IndexAlreadyExistsException e) {
            log.debug("Index exists" + indexName(type));
        } catch (IOException e) {
            log.error("Cannot create index " + indexName(type), e);
            return false;
        }
        return true;
    }

    private final ElasticsearchMapper<Status> statusMapper = new ElasticsearchMapper<Status>() {
        @Override
        public String id(Status status) {
            return status.getStatusId().toString();
        }

        @Override
        public String type() {
            return "status";
        }

        @Override
        public String prefixSearchSortField() {
            return null;
        }

        @Override
        public XContentBuilder toJson(Status status) throws IOException {
            XContentBuilder source = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("statusId", status.getStatusId())
                    .field("domain", status.getDomain())
                    .field("username", status.getUsername())
                    .field("statusDate", status.getStatusDate())
                    .field("content", status.getContent());

            if (status.getGroupId() != null) {
                Group group = groupRepository.getGroupByGroupId(UUID.fromString(status.getGroupId()));
                source.field("groupId", status.getGroupId());
                source.field("publicGroup", group.isPublicGroup());
            }
            return source.endObject();
        }
    };

    @Override
    @Async
    public void addStatus(Status status) {
        index(status, statusMapper);
    }

    @Override
    public void addStatuses(Collection<Status> statuses) {
        indexAll(statuses, statusMapper);
    }


    @Override
    public void removeStatus(Status status) {
        Assert.notNull(status, "status cannot be null");
        delete(status, statusMapper);
    }

    @Override
    public List<String> searchStatus(final String domain,
                                     final String query,
                                     int page,
                                     int size) {

        Assert.notNull(query);
        Assert.notNull(domain);

        if (page < 0) {
            page = 0; //Default value
        }
        if (size <= 0) {
            size = SearchService.DEFAULT_PAGE_SIZE;
        }

//        try {
            SearchRequestBuilder searchRequest = client().prepareSearch(indexName(statusMapper.type()))
                    .setTypes(statusMapper.type())
                    .setQuery(matchQuery(ALL_FIELD, query))
                    .setQuery(termsQuery("domain",domain))
                    .addFields()
                    .setFrom(page * size)
                    .setSize(size)
                    .addSort("statusDate", SortOrder.DESC);

            if (log.isTraceEnabled()) {
                log.trace("elasticsearch query : " + searchRequest);
            }
            SearchResponse searchResponse = searchRequest.execute().actionGet();

            SearchHits searchHits = searchResponse.getHits();
            Long hitsNumber = searchHits.totalHits();
            if (hitsNumber == 0) {
                return Collections.emptyList();
            }

            SearchHit[] hits = searchHits.hits();
            List<String> items = new ArrayList<String>(hits.length);
            for (SearchHit hit : hits) {
                items.add(hit.getId());
            }

            log.debug("search status with words ({}) = {}", query, items);
            return items;

//        } catch (IndexMissingException e) {
//            log.warn("The index " + indexName(statusMapper.type()) + " was not found in the Elasticsearch cluster.");
//            return Collections.emptyList();
//
//        } catch (ElasticSearchException e) {
//            log.error("Error happened while searching status in index " + indexName(statusMapper.type()));
//            return Collections.emptyList();
//        }
    }

    private final ElasticsearchMapper<User> userMapper = new ElasticsearchMapper<User>() {
        @Override
        public String id(User user) {
            return user.getLogin();
        }

        @Override
        public String type() {
            return "user";
        }

        @Override
        public String prefixSearchSortField() {
            return "username";
        }

        @Override
        public XContentBuilder toJson(User user) throws IOException {
            return XContentFactory.jsonBuilder()
                    .startObject()
                    .field("login", user.getLogin())
                    .field("domain", user.getDomain())
                    .field("username", user.getUsername())
                    .field("firstName", user.getFirstName())
                    .field("lastName", user.getLastName())
                    .endObject();
        }
    };

    @Override
    @Async
    public void addUser(final User user) {
        Assert.notNull(user, "user cannot be null");
        index(user, userMapper);
    }

    @Override
    public void addUsers(Collection<User> users) {
        indexAll(users, userMapper);
    }

    @Override
    public void removeUser(User user) {
        delete(user, userMapper);
    }


    @Override
    @Cacheable("user-prefix-cache")
    public Collection<String> searchUserByPrefix(String domain, String prefix) {
        return searchByPrefix(domain, prefix, DEFAULT_TOP_N_SEARCH_USER, userMapper);
    }

    private final ElasticsearchMapper<Group> groupMapper = new ElasticsearchMapper<Group>() {
        @Override
        public String id(Group group) {
            return group.getGroupId().toString();
        }

        @Override
        public String type() {
            return "group";
        }

        @Override
        public String prefixSearchSortField() {
            return "name";
        }

        @Override
        public XContentBuilder toJson(Group group) throws IOException {
            return XContentFactory.jsonBuilder()
                    .startObject()
                    .field("domain", group.getDomain())
                    .field("groupId", group.getGroupId())
                    .field("name", group.getName())
                    .field("description", group.getDescription())
                    .endObject();
        }
    };

    @Override
    @Async
    public void addGroup(Group group) {
        index(group, groupMapper);
    }

    @Override
    public void removeGroup(Group group) {
        delete(group, groupMapper);
    }

    @Override
    @Cacheable("group-prefix-cache")
    public Collection<Group> searchGroupByPrefix(String domain, String prefix, int size) {
        Collection<String> ids = searchByPrefix(domain, prefix, size, groupMapper);
        List<Group> groups = new ArrayList<Group>(ids.size());
        for (String id : ids) {
            groups.add(groupRepository.getGroupByGroupId(UUID.fromString(id)));
        }
        return groups;
    }

    @Override
    public Collection<String> searchFirstName(String firstname, int limit) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        int minimumMatch = 0;
        minimumMatch += addSearchFieldToQuery(boolQuery,"name",firstname,false);
        boolQuery.minimumShouldMatch(""+minimumMatch);

        SearchRequestBuilder searchRequest = client().prepareSearch(indexName(firstnameMapper.type()))
                .setTypes(firstnameMapper.type())
                .setQuery(boolQuery)
                .addFields()
                .setFrom(0)
                .setSize(limit);
        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse searchResponse = searchRequest
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.totalHits() == 0)
            return Collections.emptyList();

        SearchHit[] hits = searchHits.hits();
        final List<String> ids = getIdsFromSearch(hits);

        log.debug("search " + firstnameMapper.type() + " by search(\"" + firstname + ") = result : " + ids);
        return ids;
    }


    @Override
    public Collection<String> searchUserByUsernameAndFirstnameAndLastname(String domain, String username, String firstname, String lastname, boolean exact, boolean all) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(termQuery("domain",domain));
        int minimumMatch = 1;
        minimumMatch += addSearchFieldToQuery(boolQuery,"username",username,exact);
        minimumMatch += addSearchFieldToQuery(boolQuery,"firstName",firstname,exact);
        minimumMatch += addSearchFieldToQuery(boolQuery,"lastName",lastname,exact);
        boolQuery.minimumShouldMatch(""+minimumMatch);

        SearchRequestBuilder searchRequest = client().prepareSearch(indexName(userMapper.type()))
                .setTypes(userMapper.type())
                .setQuery(boolQuery)
                .addFields()
                .setFrom(0);
        if (!all) {
            searchRequest.setSize(DEFAULT_TOP_N_SEARCH_USER);
        }
        if (exact) {
            searchRequest.addSort(SortBuilders.fieldSort("username").order(SortOrder.ASC));
        }
        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse searchResponse = searchRequest
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.totalHits() == 0)
            return Collections.emptyList();

        SearchHit[] hits = searchHits.hits();
        final List<String> ids = getIdsFromSearch(hits);

        log.debug("search " + userMapper.type() + " by search(\"" + domain + "\", \"" + username + ", \"" + firstname + ", \"" + lastname + ", \") = result : " + ids);
        return ids;
    }

    private List<String> getIdsFromSearch(SearchHit[] hits) {
        return Arrays.stream(hits)
                .map(hit -> hit.getId())
                .collect(Collectors.toList());
    }

    private int addSearchFieldToQuery(BoolQueryBuilder boolQuery, String field, String value, boolean exact) {
        if (value != null) {
            if (exact) {
                boolQuery.should(matchPhraseQuery(field,value));
            } else {
                boolQuery.should(matchPhrasePrefixQuery(field,value).cutoffFrequency(0.001F));
            }
            return 1;
        }
        return 0;
    }

    /**
     * Indexes an object to elasticsearch.
     * This method is asynchronous.
     *
     * @param object Object to index.
     * @param mapper Converter to JSON.
     */
    private <T> void index(T object, ElasticsearchMapper<T> mapper) {
        Assert.notNull(object);
        Assert.notNull(mapper);

        final String type = mapper.type();
        final String id = mapper.id(object);
        try {
            final XContentBuilder source = mapper.toJson(object);

            log.debug("Ready to index the {} id {} into Elasticsearch: {}", type, id, stringify(source));
            client()
                    .prepareIndex(indexName(type), type, id)
                    .setSource(source)
                    .execute(getESActionListener(type, id, source));
        } catch (IOException e) {
            log.error("The " + type + " id " + id + " wasn't indexed", e);
        }
    }

    /**
     * Indexes an collection of objects to elasticsearch.
     * This method is synchronous.
     *
     * @param collection Object to index.
     * @param adapter    Converter to JSON.
     */
    private <T> void indexAll(Collection<T> collection, ElasticsearchMapper<T> adapter) {
        Assert.notNull(collection);
        Assert.notNull(adapter);

        if (collection.isEmpty())
            return;

        String type = adapter.type();
        BulkRequestBuilder request = client().prepareBulk();

        for (T object : collection) {
            String id = adapter.id(object);
            try {
                XContentBuilder source = adapter.toJson(object);
                IndexRequestBuilder indexRequest = client().prepareIndex(indexName(type), type, id).setSource(source);
                request.add(indexRequest);

            } catch (IOException e) {
                log.error("The " + type + " of id " + id + " wasn't indexed", e);
            }
        }

        log.debug("Ready to index {} {} into Elasticsearch.", collection.size(), type);

        BulkResponse response = request.execute().actionGet();
        if (response.hasFailures()) {
            int errorCount = 0;
            for (BulkItemResponse itemResponse : response) {
                if (itemResponse.isFailed()) {
                    log.error("The " + type + " of id " + itemResponse.getId() + " wasn't indexed in bulk operation: " + itemResponse.getFailureMessage());
                    ++errorCount;
                }
            }
            log.error(errorCount + " " + type + " where not indexed in bulk operation.");

        } else {
            log.debug("{} {} indexed into Elasticsearch in bulk operation.", collection.size(), type);
        }
    }

    /**
     * delete a document.
     * This method is asynchronous.
     *
     * @param object Object to index.
     * @param mapper Converter to JSON.
     */
    private <T> void delete(T object, ElasticsearchMapper<T> mapper) {
        Assert.notNull(object);
        Assert.notNull(mapper);

        final String id = mapper.id(object);
        final String type = mapper.type();

        log.debug("Ready to delete the {} of id {} from Elasticsearch: ", type, id);

        client().prepareDelete(indexName(type), type, id).execute(getDeleteListener(id, type));
    }

    private ActionListener<DeleteResponse> getDeleteListener(final String id, final String type) {
        return new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
                if (log.isDebugEnabled()) {
                    if (!deleteResponse.isFound()) {
                        log.debug("{} of id {} was not found therefore not deleted.", type, id);
                    } else {
                        log.debug("{} of id {} was deleted from Elasticsearch.", type, id);
                    }
                }
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("The " + type + " of id " + id + " wasn't deleted from Elasticsearch.", e);
            }
        };
    }

    public Collection<String> searchByUsername(String domain, String prefix, int size) {

        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(termQuery("domain",domain));
        boolQuery.should(matchQuery("username",prefix));
        boolQuery.minimumShouldMatch("2");

        SearchRequestBuilder searchRequest = client().prepareSearch(indexName(userMapper.type()))
                .setTypes(userMapper.type())
                .setQuery(boolQuery)
                .addFields()
                .setFrom(0)
                .setSize(size);

        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse searchResponse = searchRequest
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.totalHits() == 0)
            return Collections.emptyList();

        SearchHit[] hits = searchHits.hits();
        final List<String> ids = getIdsFromSearch(hits);

        log.debug("search " + userMapper.type() + " by prefix(\"" + domain + "\", \"" + prefix + "\") = result : " + ids);
        return ids;


    }
    private Collection<String> searchByPrefix(String domain, String prefix, int size, ElasticsearchMapper<?> mapper) {

            SearchRequestBuilder searchRequest = client().prepareSearch(indexName(mapper.type()))
                    .setTypes(mapper.type())
                    .setQuery(matchQuery("prefix", prefix))
                    .setQuery(termQuery("domain", domain))
                    .addFields()
                    .setFrom(0)
                    .setSize(size)
                    .addSort(SortBuilders.fieldSort(mapper.prefixSearchSortField()).order(SortOrder.ASC));

            if (log.isTraceEnabled()) {
                log.trace("elasticsearch query : " + searchRequest);
            }
            SearchResponse searchResponse = searchRequest
                    .execute()
                    .actionGet();

            SearchHits searchHits = searchResponse.getHits();
            if (searchHits.totalHits() == 0)
                return Collections.emptyList();

            SearchHit[] hits = searchHits.hits();
            final List<String> ids = getIdsFromSearch(hits);

            log.debug("search " + mapper.type() + " by prefix(\"" + domain + "\", \"" + prefix + "\") = result : " + ids);
            return ids;


    }

    /**
     * Stringify a document source for logging purpose.
     *
     * @param source Source of the document.
     * @return A string representation of the document only valid for logging purpose.
     */
    private String stringify(XContentBuilder source) {
        try {
            return source.prettyPrint().string();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Used to transform an object to it's indexed representation.
     */
    private static interface ElasticsearchMapper<T> {
        /**
         * Provides object id;
         *
         * @param o object.
         * @return object id.
         */
        String id(T o);

        /**
         * Provides index type of this mapping.
         *
         * @return The elasticsearch index type of the object.
         */
        String type();

        /**
         * @return The name of the field to sort by in search by prefix.
         */
        String prefixSearchSortField();

        /**
         * Convert object to it's indexable JSON document representation.
         *
         * @param o object.
         * @return Document
         * @throws IOException If the creation of the JSON document failed.
         */
        XContentBuilder toJson(T o) throws IOException;
    }

    private final ElasticsearchMapper<String> firstnameMapper = new ElasticsearchMapper<String>() {
        @Override
        public String id(String firstname) {
            return firstname;
        }

        @Override
        public String type() {
            return "firstname";
        }

        @Override
        public String prefixSearchSortField() {
            return "name";
        }

        @Override
        public XContentBuilder toJson(String firstname) throws IOException {
            return XContentFactory.jsonBuilder()
                    .startObject()
                    .field("name", firstname)
                    .endObject();
        }
    };
    @Override
    @Async
    public void addFirstName(final String firstname) {
        Assert.notNull(firstname, "user cannot be null");
        index(firstname, firstnameMapper);
    }

    @Override
    @Async
    public void addFirstnames(Collection<String> firstnames) {
        if (!client().admin().indices().prepareExists(indexName("firstname")).execute().actionGet().isExists()) {
            log.info("Index {} does not exists in Elasticsearch, creating it!", indexName("firstname"));
            createSpecificIndex("firstname");
        } else {
            boolean ack = client().admin().indices().prepareDelete(indexName("firstname")).execute().actionGet().isAcknowledged();
            if (!ack) {
                log.error("Elasticsearch Index wasn't deleted !");
                return;
            } else {
                createSpecificIndex("firstname");
            }
        }
        indexAll(firstnames, firstnameMapper);
    }

    @Override
    public void removeFirstname(String firstname) {
        delete(firstname, firstnameMapper);
    }


    @Override
    public List<UserFavouriteCountDTO> countUsersForUserFavourites(List<String> favourites, User user) {
        List<String> logins = new ArrayList<>();
        if (user != null) {
            logins = friendRepository.findFriendsForUser(user.getLogin());
        }

        final Map<String,Long> total = getCountUserFavourites(favourites, null);
        final Map<String, Long> friendTotal = getFriendTotal(favourites, logins);
        return total.keySet()
                .stream()
                .map(id -> new UserFavouriteCountDTO(id,total.get(id),friendTotal.get(id)))
                .collect(toList());
    }

    private Map<String, Long> getFriendTotal(List<String> favourites, List<String> logins) {
        Map<String, Long> friendTotal = new HashMap<>();
        if (logins != null && !logins.isEmpty()) {
            friendTotal = getCountUserFavourites(favourites, logins);
        }
        return friendTotal;
    }

    private Map<String, Long> getCountUserFavourites(List<String> favourites, List<String> logins) {
        AggregationBuilder aggregation =
                AggregationBuilders
                        .terms("aggregated").field("_parent");
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(termsQuery("favourite",favourites));
        if (logins != null) {
            boolQuery.must(termsQuery("login", logins.stream()
            .map(QueryParser::escape)
            .collect(Collectors.toList())));
        }
        SearchRequestBuilder searchRequest = client().prepareSearch()
                .setQuery(boolQuery)
                .addAggregation(aggregation);
        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse sr = searchRequest.execute().actionGet();

        sr.getAggregations().get("aggregated");

        Terms result = sr.getAggregations().get("aggregated");
        Map<String,Long> favouriteCount = new HashMap<>();
        if (result != null) {
            favourites.stream().forEach(favourite ->
                    addCountForFavouriteConsumer(favourite, result, favouriteCount));
        }
        return favouriteCount;
    }

    private void addCountForFavouriteConsumer(String favourite, Terms result, Map<String, Long> favouriteCount) {
        if (result.getBucketByKey(favourite) != null) {
            favouriteCount.put(favourite,result.getBucketByKey(favourite).getDocCount());
        }
    }

    /**
     * Indexes a user to favourite.
     * This method is asynchronous.
     *
     * @param favourite that needs to be indexed.
     * @param login that needs to be indexed.
     */
    @Override
    @Async
    public void indexUserFavourite(String favourite, String login) {
        Assert.notNull(favourite);
        Assert.notNull(login);

        String type = "favourite";
        String index = indexName(type);
        String id = favourite;
        try {
            addFavouriteToIndex(favourite, type, index, id);
            addUserFavouriteToIndex(favourite, login, index);
        } catch (IOException e) {
            log.error("The " + type + " id " + id + " wasn't indexed", e);
        }
    }

    @Override
    public void removeUserFavourite(String favourite, String login) {
        String type = "user";
        String index = indexName("favourite");
        String id = login+"-"+favourite;

        log.debug("Ready to delete the {} of id {} and parent {} from Elasticsearch: ", type, id, favourite);

        client().prepareDelete(index, type, id).setParent(favourite).execute(getDeleteListener(id, type));

    }

    @Override
    public List<String> getFriendsForUserFavourite(String id, User user, int from, int size) {
        List<String> logins = new ArrayList<>();
        if (user != null) {
            logins = friendRepository.findFriendsForUser(user.getLogin());
        }

        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(termsQuery("login",logins));
        boolQuery.must(termQuery("favourite",id));

        SearchRequestBuilder searchRequest = client().prepareSearch(indexName("favourite"))
                .setTypes("user")
                .setQuery(boolQuery)
                .addFields()
                .setFrom(from);
        if (size > 0 && size < logins.size()) {
            searchRequest.setSize(size+1);
        } else {
            searchRequest.setSize(logins.size());
        }
        searchRequest.addSort(SortBuilders.fieldSort("login").order(SortOrder.DESC));
        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse searchResponse = searchRequest
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.totalHits() == 0)
            return Collections.emptyList();

        SearchHit[] hits = searchHits.hits();
        List<String> ids = getLoginsFromHits(hits, "login");

        log.debug("search favourites " + " by search(\"" + id + "\", \"" + logins + "\") = result : " + ids);
        return ids;
    }

    @Override
    public Collection<String> getUserFavouritesForUser(String username, String domain) {
        List<Username> usernames = usernameRepository.findUsernamesByDomainAndUsername(domain,username);
        if (usernames != null && !usernames.isEmpty()) {
            return getUserFavourites(usernames.get(0));
        }
        return new ArrayList<>();
    }

    @Override
    public Ping createElasticSearchPing(Ping ping) {
        long start = System.currentTimeMillis();
        if (ping == null) {
            ping = new Ping();
        }
        SearchRequestBuilder searchRequest = client().prepareSearch(indexName(userMapper.type()))
                .setTypes(userMapper.type())
                .addFields()
                .setFrom(0)
                .setSize(1);

        SearchResponse searchResponse = searchRequest
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        ping.setElasticSearch(System.currentTimeMillis()-start);
        return ping;
    }

    private Collection<String> getUserFavourites(Username username) {
        SearchRequestBuilder searchRequestBuilder = client().prepareSearch(indexName("favourite"));

        //Query 1. Search on all books that have the term 'book' in the title and return the 'authors'.
        HasChildQueryBuilder favouriteHasChildQuery = QueryBuilders.hasChildQuery("user", QueryBuilders.matchQuery("login", QueryParser.escape(username.getLogin())));
        SearchRequestBuilder searchRequest = searchRequestBuilder.setQuery(favouriteHasChildQuery);
        if (log.isTraceEnabled()) {
            log.trace("elasticsearch query : " + searchRequest);
        }
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        SearchHits searchHits = searchResponse.getHits();
        if (searchHits.totalHits() == 0)
            return Collections.emptyList();

        SearchHit[] hits = searchHits.hits();
        List<String> ids = getLoginsFromHits(hits, "favourite");
        log.debug("search favourites " + " for user(\"" + username.getLogin() + "\") = result : " + ids);
        return ids;
    }

    private List<String> getLoginsFromHits(SearchHit[] hits, String type) {
        if (hits == null || hits.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(hits)
                    .map(hit -> hit.getFields().get(type).getValue().toString())
                    .collect(Collectors.toList());
    }


    private void addUserFavouriteToIndex(String favourite, String login, String index) throws IOException {
        String id = login+"-"+favourite;
        final String type = "user";
        XContentBuilder userFavouriteJson = XContentFactory.jsonBuilder()
                .startObject()
                .field("id", id)
                .field("login", login)
                .field("favourite", favourite)
                .endObject();

        log.debug("Ready to index the {} id {} into Elasticsearch: {}", type, id, stringify(userFavouriteJson));
        client().prepareIndex(index, type, id).setSource(userFavouriteJson).setParent(favourite).execute(getESActionListener(type, id, userFavouriteJson));
    }

    private void addFavouriteToIndex(final String favourite, final String type, final String index, final String id) throws IOException {
        final XContentBuilder favouriteJson = XContentFactory.jsonBuilder()
                .startObject()
                .field("id", favourite)
                .endObject();

        log.debug("Ready to index the {} id {} into Elasticsearch: {}", type, id, stringify(favouriteJson));
        client().prepareIndex(index, type, id).setSource(favouriteJson).execute(getESActionListener(type, id, favouriteJson));
    }


    private ActionListener<IndexResponse> getESActionListener(final String type, final String id, final XContentBuilder favouriteJson) {
        return new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse response) {
                log.debug(type + " id " + id + " was " + (response.getVersion() == 1 ? "indexed" : "updated") + " into Elasticsearch");
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("The " + type + " id " + id + " wasn't indexed : " + stringify(favouriteJson), e);
            }
        };
    }

}
