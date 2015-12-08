package fr.ippon.tatami.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;

@Configuration
@PropertySource({"classpath:/META-INF/tatami/tatami.properties",
        "classpath:/META-INF/tatami/customization.properties"})
@ComponentScan(basePackages = {
        "fr.ippon.tatami.repository",
        "fr.ippon.tatami.service",
        "fr.ippon.tatami.security"})
@Import(value = {
        AsyncConfiguration.class,
//        CacheConfiguration.class,
        CassandraConfiguration.class,
        SearchConfiguration.class,
        MailConfiguration.class,
        MetricsConfiguration.class})
@ImportResource("classpath:META-INF/spring/applicationContext-*.xml")
public class ApplicationConfiguration {

    private final Logger log = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Inject
    private Environment env;

    public static final String DEFAULT_INCLUDE_PATTERN = "/tatami/.*";

    @Bean
    public Docket swaggerSpringfoxDocket() {
        log.debug("Starting Swagger");
        StopWatch watch = new StopWatch();
        watch.start();
        ApiInfo apiInfo = new ApiInfo(
                env.getProperty("swagger.title"),
                env.getProperty("swagger.description"),
                env.getProperty("swagger.version"),
                env.getProperty("swagger.termsOfServiceUrl"),
                env.getProperty("swagger.contact"),
                env.getProperty("swagger.license"),
                env.getProperty("swagger.licenseUrl"));

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .directModelSubstitute(LocalDate.class,
                        String.class)
                .genericModelSubstitutes(ResponseEntity.class)
                .useDefaultResponseMessages(false)
                .enableUrlTemplating(true)
                .pathMapping("/tatami");
        watch.stop();
        log.debug("Started Swagger in {} ms", watch.getTotalTimeMillis());
        return docket;
    }


    /**
     * Initializes Tatami.
     * <p/>
     * Spring profiles can be configured with a system property -Dspring.profiles.active=your-active-profile
     * <p/>
     * Available profiles are :
     * - "apple-push" : for enabling Apple Push notifications
     * - "metrics" : for enabling Yammer Metrics
     * - "tatamibot" : for enabling the Tatami bot
     */
    @PostConstruct
    public void initTatami() throws IOException {
        log.debug("Looking for Spring profiles... Available profiles are \"metrics\", \"tatamibot\" and \"apple-push\"");
        if (env.getActiveProfiles().length == 0) {
            log.debug("No Spring profile configured, running with default configuration");
        } else {
            for (String profile : env.getActiveProfiles()) {
                log.debug("Detected Spring profile : " + profile);
            }
        }
        Constants.VERSION = env.getRequiredProperty("tatami.version");
        Constants.GOOGLE_ANALYTICS_KEY = env.getProperty("tatami.google.analytics.key");
        String canLoginString = env.getProperty("tatami.user.deactivated.can.login","false");
        Constants.DEACTIVATED_USER_CAN_LOGIN = Boolean.valueOf(canLoginString);
        Constants.MODERATOR_STATUS = Boolean.valueOf(env.getProperty("tatami.moderator.status","false"));

        log.info("Tatami v. {} started!", Constants.VERSION);
        log.debug("Google Analytics key : {}", Constants.GOOGLE_ANALYTICS_KEY);

    }
}
