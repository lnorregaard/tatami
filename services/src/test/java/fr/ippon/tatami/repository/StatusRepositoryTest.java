package fr.ippon.tatami.repository;

import fr.ippon.tatami.AbstractCassandraTatamiTest;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.status.AbstractStatus;
import fr.ippon.tatami.domain.status.Status;
import org.junit.Test;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.util.ArrayList;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class StatusRepositoryTest extends AbstractCassandraTatamiTest {

    @Inject
    public StatusRepository statusRepository;

    @Test
    public void shouldGetAStatusRepositoryInjected() {
        assertThat(statusRepository, notNullValue());
    }

    @Test
    public void shouldCreateAStatus() {
        String login = "jdubois@ippon.fr";
        String content = "content";

        Status created = statusRepository.createStatus(login, false, null, new ArrayList<>(),
                content, "", "", "", "48.54654, 3.87987987", false);
        log.info(created.getStatusId().toString());
        AbstractStatus foundStatus =  statusRepository.findStatusById(created.getStatusId().toString());
        assertThat(foundStatus, notNullValue());
    }

    @Test
    public void shouldCreateAStatusPending() {
        Constants.MODERATOR_STATUS = true;
        String login = "jdubois@ippon.fr";
        String content = "content";

        Status created = statusRepository.createStatus(login, false, null, new ArrayList<>(),
                content, "", "", "", "48.54654, 3.87987987", false);
        log.info(created.getStatusId().toString());
        AbstractStatus found = statusRepository.findStatusById(created.getStatusId().toString(),false);
        assertThat(found, notNullValue());
        assertEquals(found.getState(),"PENDING");

        AbstractStatus foundStatus =  statusRepository.findStatusById(created.getStatusId().toString());
        assertThat(foundStatus, nullValue());
        Constants.MODERATOR_STATUS = false;
    }

    @Test(expected = ValidationException.class)
    public void shouldNotCreateAStatusBecauseLoginNull() {
        String login = null;
        String content = "content";

        Status status = new Status();
        status.setContent(content);
        status.setLogin(login);

        statusRepository.createStatus(login, false, null, new ArrayList<String>(),
                content, "", "", "", null, false);
    }

    @Test(expected = ConstraintViolationException.class)
    public void shouldNotCreateAStatusBecauseContentNull() {
        String login = "jdubois@ippon.fr";
        String username = "jdubois";
        String domain = "ippon.fr";
        String content = null;

        Status status = new Status();
        status.setContent(content);
        status.setLogin(login);

        statusRepository.createStatus(login, false, null, new ArrayList<String>(),
                content, "", "", "", null, false);
    }

    @Test(expected = ConstraintViolationException.class)
    public void shouldNotCreateAStatusBecauseContentEmpty() {
        String login = "jdubois@ippon.fr";
        String username = "jdubois";
        String domain = "ippon.fr";
        String content = "";

        Status status = new Status();
        status.setContent(content);
        status.setLogin(login);

        statusRepository.createStatus(login, false, null, new ArrayList<String>(),
                content, "", "", "", null, false);
    }

    @Test(expected = ConstraintViolationException.class)
    public void shouldNotCreateAStatusBecauseContentTooLarge() {
        String tmp = "0123456789";
        String content = "";
        for (int i = 0; i < 410; i++) {
            content += tmp;
        }

        Status status = new Status();
        status.setContent(content);
        String login = "jdubois@ippon.fr";
        String username = "jdubois";
        String domain = "ippon.fr";

        statusRepository.createStatus(login, false, null, new ArrayList<String>(),
                content, "", "", "", null, false);
    }
}