package fr.ippon.tatami.service.elasticsearch;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Transport client configuration.
 */
public class EmbeddedElasticsearchEngine implements ElasticsearchEngine {

    private final Logger log = LoggerFactory.getLogger(EmbeddedElasticsearchEngine.class);

    @Inject
    private Environment env;

    private Node node;

    @PostConstruct
    private void init() {
        log.info("Initializing Elasticsearch embedded cluster...");

        node = nodeBuilder()
                .settings(Settings.builder().loadFromSource("META-INF/elasticsearch/elasticsearch-embedded.yml").put("path.home", env.getProperty("tatami.elasticsearch.path.data")+"/"))

                .node();

        // Looking for nodes configuration
        if (log.isInfoEnabled()) {
            final NodesInfoResponse nir =
                    client().admin().cluster().prepareNodesInfo().execute().actionGet();

            log.info("Elasticsearch client is now connected to the " + nir.getNodes().length + " node(s) cluster named \""
                    + nir.getClusterName() + "\"");
        }
    }

    @PreDestroy
    private void close() {
        log.info("Closing Elasticsearch embedded cluster");
        node.close();
    }

    public Client client() {
        return node.client();
    }
}