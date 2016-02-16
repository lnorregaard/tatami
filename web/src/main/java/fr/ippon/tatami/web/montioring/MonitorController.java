package fr.ippon.tatami.web.montioring;

import com.yammer.metrics.annotation.Timed;
import fr.ippon.tatami.domain.Ping;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;

/*
 * REST controller for managing users.
 * @author Arthur Weber
 */
@Controller
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @Inject
    private MonitorService monitiorService;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    Environment env;

    @RequestMapping(value = "/ping",
            method = RequestMethod.GET)
    @ResponseBody
    @Timed
    public Ping ping() {
        return monitiorService.ping();
    }


}
