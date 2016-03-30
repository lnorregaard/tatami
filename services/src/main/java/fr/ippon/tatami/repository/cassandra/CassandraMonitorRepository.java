package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import fr.ippon.tatami.domain.Ping;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.validation.ContraintsUserCreation;
import fr.ippon.tatami.repository.CounterRepository;
import fr.ippon.tatami.repository.MonitorRepository;
import fr.ippon.tatami.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * Cassandra implementation of the user repository.
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraMonitorRepository implements MonitorRepository {

    private final Logger log = LoggerFactory.getLogger(CassandraMonitorRepository.class);

    @Inject
    private Session session;

    @Override
    public Ping createCassandraPing(Ping ping) {
        long start = System.currentTimeMillis();
        if (!session.getState().getConnectedHosts().isEmpty()) {
            if (ping == null) {
                Ping p = new Ping();
                ping = p;
            }
            ping.setCassandra(System.currentTimeMillis()-start);
            if (ping.getCassandra() <= 0) {
                ping.setCassandra(1);
            }
        } else {
            return null;
        }
        return ping;
    }
}
