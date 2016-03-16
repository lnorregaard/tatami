package fr.ippon.tatami.service;

import fr.ippon.tatami.config.Constants;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by lnorregaard on 16/03/16.
 */
@Service
public class StatusService {

    @Inject
    TimelineService timelineService;

    public void removeStatusForDeletedUser(String statusId) {
        timelineService.removeStatusForDeletedUser(statusId);
    }

    @Async
    public void removeStatusesForDeletedUser(List<String> statuses) {
        try {
            Constants.DELETE_USER_FORK_JOIN_POOL.submit(() ->
                    statuses.parallelStream().forEach((statusId) -> removeStatusForDeletedUser(statusId))
            ).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
