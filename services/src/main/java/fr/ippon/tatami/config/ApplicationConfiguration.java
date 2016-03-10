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
@PropertySources({
        @PropertySource({"classpath:/META-INF/tatami/tatami.properties",
                "classpath:/META-INF/tatami/customization.properties"}),
        @PropertySource(value = "file:${CONF_DIR}/tatami.properties", ignoreResourceNotFound = true)})
@ComponentScan(basePackages = {
        "fr.ippon.tatami.repository",
        "fr.ippon.tatami.service",
        "fr.ippon.tatami.security"})
@Import(value = {
        AsyncConfiguration.class,
        CacheConfiguration.class,
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
                .enableUrlTemplating(true);
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
        Constants.ANONYMOUS_SHOW_GROUP_TIMELINE = Boolean.valueOf(env.getProperty("tatami.anonymous.show.group.timeline","false"));
        Constants.NON_GROUP_MEMBER_POST_TIMELINE = Boolean.valueOf(env.getProperty("tatami.non.group.member.post.timeline","false"));
        Constants.USER_AND_FRIENDS = Boolean.valueOf(env.getProperty("tatami.user.rename.and.friends.request","false"));
        Constants.LOCAL_ATTACHMENT_STORAGE = Boolean.valueOf(env.getProperty("attachment.storage.local","false"));
        Constants.ATTACHMENT_WEB_PREFIX = env.getProperty("attachment.web.prefix","");
        Constants.ATTACHMENT_FILE_PATH = env.getProperty("attachment.file.path","");
        Constants.ATTACHMENT_DIR_PREFIX = Integer.parseInt(env.getProperty("attachment.directory.prefix.number","2"));
        Constants.ATTACHMENT_THUMBNAIL_NAME = env.getProperty("attachment.thumbnail.name","_thumb");
        Constants.ATTACHMENT_IMAGE_WIDTH = Integer.parseInt(env.getProperty("attachment.image.width","-1"));
        Constants.AVATAR_THUMBNAIL_SIZE = Integer.parseInt(env.getProperty("avatar.thumbnail.size","126"));
        String basicSize = "storage.basic.max.size";
        String premiumSize = "storage.premium.max.size";
        String ipponSize = "storage.ippon.max.size";
        String basicSuscription = "suscription.level.free";
        String premiumSuscription = "suscription.level.premium";
        String ipponSuscription = "suscription.level.ippon";

        Constants.STORAGE_BASICSIZE= Integer.parseInt(env.getProperty(basicSize,"100"));
        Constants.STORAGE_PREMIUMSIZE= Integer.parseInt(env.getProperty(premiumSize,"1000"));
        Constants.STORAGE_IPPONSIZE= Integer.parseInt(env.getProperty(ipponSize,"10000"));
        Constants.STORAGE_BASICSUSCRIPTION= Integer.parseInt(env.getProperty(basicSuscription,"10"));
        Constants.STORAGE_PREMIUMSUSCRIPTION= Integer.parseInt(env.getProperty(premiumSuscription,"10"));
        Constants.STORAGE_IPPONSUSCRIPTION= Integer.parseInt(env.getProperty(ipponSuscription,"10"));

        log.info("Tatami v. {} started!", Constants.VERSION);
        log.debug("Google Analytics key : {}", Constants.GOOGLE_ANALYTICS_KEY);

    }
}
