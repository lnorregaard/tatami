package fr.ippon.tatami.service;

import fr.ippon.tatami.AbstractCassandraTatamiTest;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.status.AbstractStatus;
import fr.ippon.tatami.domain.status.Status;
import fr.ippon.tatami.repository.GroupRepository;
import fr.ippon.tatami.repository.StatusRepository;
import fr.ippon.tatami.repository.StatusStateGroupRepository;
import org.junit.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ModeratorTimelineServiceTest extends AbstractCassandraTatamiTest {

    @Inject
    public TimelineService timelineService;

    @Inject
    public StatusRepository statusRepository;

    @Inject
    public StatusStateGroupRepository statusStateGroupRepository;

    @Inject
    public GroupRepository groupRepository;

    private UUID groupId = null;
    private Group group = null;
    private Status status = null;

    @Before
    public void setup() {
        Constants.MODERATOR_STATUS = true;
        groupId = groupRepository.createGroup("ippon.fr","test","test",true);
        group = groupRepository.getGroupById("ippon.fr",groupId);
    }
    @Test
    public void addStatusPending() {
        String login = "jdubois@ippon.fr";
        String content = "content";

        status = statusRepository.createStatus(login, false, group, new ArrayList<>(),
                content, "", "", "", "48.54654, 3.87987987", false);
        log.info(status.getStatusId().toString());
        assertThat(status.getState(), is("PENDING"));
    }

    @Test
    public void blockStatus() {
        addStatusPending();
        timelineService.blockStatus("jdubois@ippon.fr",status.getStatusId().toString(),"blocked status","jdubois@ippon.fr");
        AbstractStatus statusNew = statusRepository.findStatusById(status.getStatusId().toString(),false);
        List<UUID> list = statusStateGroupRepository.findStatuses("BLOCKED",group.getGroupId().toString(),null,null,10);
        assertThat(list, notNullValue());
        assertThat(list.size(), is(1));
        assertThat(list.iterator().next(),is(status.getStatusId()));
        assertThat(statusNew.getState(),is("BLOCKED"));
    }

    @Test
    public void approveStatus() {
        addStatusPending();
        timelineService.approveStatus(status.getStatusId().toString());
        AbstractStatus statusNew = statusRepository.findStatusById(status.getStatusId().toString(),true);
        List<UUID> list = statusStateGroupRepository.findStatuses("APPROVED",group.getGroupId().toString(),null,null,10);
        assertThat(list, notNullValue());
        assertThat(list.size(), is(1));
        assertThat(list.iterator().next(),is(status.getStatusId()));
        assertThat(statusNew.getState(),nullValue());
    }

    @After
    public void takeDown() {
        if (status != null) {
            statusRepository.removeStatus(status);
        }
        Constants.MODERATOR_STATUS = false;
    }

}