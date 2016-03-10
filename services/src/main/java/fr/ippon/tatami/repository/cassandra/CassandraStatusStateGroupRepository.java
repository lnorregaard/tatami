package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import fr.ippon.tatami.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import javax.validation.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * Cassandra implementation of the moderated status repository.
 * <p/>
 *
 * @author Lars Nørregaard
 */
@Repository
public class CassandraStatusStateGroupRepository implements StatusStateGroupRepository {
    private static final Logger log = LoggerFactory.getLogger(CassandraStatusStateGroupRepository.class);

    public static final String STATUS_STATE_GROUP_LINE = "statusstategroupline";
    public static final String STATUS_ID = "statusId";
    public static final String STATE = "state";
    //    private final Logger log = LoggerFactory.getLogger(CassandraStatusStateGroupRepository.class);

    private static final String GROUP_ID = "groupId";
    public static final String GROUP_NULL = "GROUP_NULL";
    public static final String PENDING = "PENDING";
    public static final String BLOCKED = "BLOCKED";
    public static final String APPROVED = "APPROVED";
    public static final List<String> STATES = Arrays.asList(PENDING, BLOCKED, APPROVED);

    @Inject
    private Session session;


    private final static int COLUMN_TTL = 60 * 60 * 24 * 90; // The column is stored for 90 days.

    @Override
    public void createStatusStateGroup(UUID statusId, String state, String groupId) throws ConstraintViolationException {
        if (state == null) {
            state = PENDING;
        }
        Insert insert = QueryBuilder.insertInto(STATUS_STATE_GROUP_LINE)
                .value(STATUS_ID, statusId)
                .value(STATE, state);
                if (groupId != null) {
                    insert.value(GROUP_ID,groupId);
                } else {
                    insert.value(GROUP_ID, GROUP_NULL);
                }
        log.debug("Insert state statusId: {}, state: {}, groupId: {}, statement: {} ", statusId,state,groupId,insert.toString());
        session.execute(insert.using(ttl(COLUMN_TTL)));
    }

    @Override
    public void updateState(String groupId, UUID statusId, String newState) {
        log.debug("update state for groupId : {}, statusId: {} to state: {}",groupId,statusId,newState);
        if (groupId == null) {
            groupId = GROUP_NULL;
        }
        Statement statement = QueryBuilder.delete().from(STATUS_STATE_GROUP_LINE)
                .where(eq(GROUP_ID,groupId))
                .and(in(STATE,STATES))
                .and(eq(STATUS_ID, statusId));
        log.debug("execute statement : {}", statement.toString());
        session.execute(statement);
        if (newState != null) {
            createStatusStateGroup(statusId, newState, groupId);
        }
    }

    private List<String> withoutState(List<String> states, String newState) {
        if (newState == null) {
            return states;
        } else {
            return states.stream()
                    .filter(s -> !s.equals(newState))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<UUID> findStatuses(String types, String groupId, UUID from, UUID finish, int count) {
        List<String> states = new ArrayList<>();
        if (types != null && types.contains(",")) {
            states = Arrays.asList(types.split(","));
        } else if (types != null) {
            states.add(types);
        } else {
            states = STATES;
        }
        Select select = QueryBuilder.select()
                .column(STATUS_ID)
                .from(STATUS_STATE_GROUP_LINE);
        Select.Where where = null;
        if (groupId != null) {
            where = addWhere(select,where,eq(GROUP_ID,groupId));
        } else {
            where = addWhere(select,where,eq(GROUP_ID,GROUP_NULL));
        }
        where = addWhere(select,where,in(STATE,states));
        if(finish != null) {
            where = addWhere(select,where,lt(STATUS_ID,finish));
        } else if(from != null) {
            where = addWhere(select,where,gt(STATUS_ID,finish));
        }
        if (count > 0) {
            where.limit(count);
        }
        where.orderBy(asc(STATUS_ID));
        ResultSet results = session.execute(where);
        return results
                .all()
                .stream()
                .map(e -> e.getUUID(STATUS_ID))
                .collect(Collectors.toList());
    }

    @Override
    public Long findStatusesCount(String types, String groupId) {
        List<String> states = new ArrayList<>();
        if (types != null && types.contains(",")) {
            states = Arrays.asList(types.split(","));
        } else if (types != null) {
            states.add(types);
        } else {
            states = STATES;
        }
        Select select = QueryBuilder.select().countAll()
                .from(STATUS_STATE_GROUP_LINE);
        Select.Where where = null;
        if (groupId != null) {
            where = addWhere(select,where,eq(GROUP_ID,groupId));
        } else {
            where = addWhere(select,where,eq(GROUP_ID,GROUP_NULL));
        }
        where = addWhere(select,where,in(STATE,states));
        ResultSet results = session.execute(where);
        return results
                .one()
                .getLong(0);
    }

    @Override
    public void removeState(String groupId, UUID statusId) {
        Statement statement = QueryBuilder.select()
                .all()
                .from(STATUS_STATE_GROUP_LINE)
                .allowFiltering()
                .where(eq(STATUS_ID, statusId));
        log.debug("execute statement : {}", statement.toString());
        ResultSet results = session.execute(statement);
        if (!results.isExhausted()) {
            Row row = results
                    .one();
            Statement deleteStatement = QueryBuilder.delete().from(STATUS_STATE_GROUP_LINE)
                    .where(eq(GROUP_ID,row.getString(GROUP_ID)))
                    .and(eq(STATE,row.getString(STATE)))
                    .and(eq(STATUS_ID, row.getUUID(STATUS_ID)));
            log.debug("execute delete statement : {}", deleteStatement.toString());
            session.execute(deleteStatement);
        }
    }

    private Select.Where addWhere(Select select, Select.Where where, Clause clause) {
        if (where == null) {
            return select.where(clause);
        } else {
            return where.and(clause);
        }
    }
}
