package fr.ippon.tatami.config;

import java.util.concurrent.ForkJoinPool;

/**
 * Application constants.
 */
public class Constants {


    public static int ATTACHMENT_DIR_PREFIX = 2;

    private Constants() {
    }

    public static final String SPRING_PROFILE_METRICS = "metrics";

    public static final String REMOTE_ENGINE = "remote";

    public static final String EMBEDDED_ENGINE = "embedded";

    public static String VERSION = null;

    public static String GOOGLE_ANALYTICS_KEY = null;

    public static final int PAGINATION_SIZE = 50;

    public static final String TATAMIBOT_NAME = "tatamibot";

    public static final int AVATAR_SIZE = 230;


    /**
     * Cassandra : number of columns to return when not doing a name-based template
     */
    public static final int CASSANDRA_MAX_COLUMNS = 10000;

    /**
     * Cassandra : number of rows to return
     */
    public static final int CASSANDRA_MAX_ROWS = 10000;

    public static boolean DEACTIVATED_USER_CAN_LOGIN = false;

    public static boolean ANONYMOUS_SHOW_GROUP_TIMELINE = false;
    public static boolean NON_GROUP_MEMBER_POST_TIMELINE = false;
    public static boolean USER_AND_FRIENDS = false;
    public static boolean MODERATOR_STATUS = false;

    public static boolean LOCAL_ATTACHMENT_STORAGE = false;
    public static String ATTACHMENT_WEB_PREFIX = "";
    public static String ATTACHMENT_FILE_PATH = "";
    public static String ATTACHMENT_THUMBNAIL_NAME = "_thumb";
    public static int ATTACHMENT_IMAGE_WIDTH = -1;
    public static int AVATAR_THUMBNAIL_SIZE = 126;


    public static int STORAGE_BASICSIZE = 10;
    public static int STORAGE_PREMIUMSIZE = 1000;
    public static int STORAGE_IPPONSIZE = 100000;
    public static int STORAGE_BASICSUSCRIPTION = 0;
    public static int STORAGE_PREMIUMSUSCRIPTION = 1;
    public static int STORAGE_IPPONSUSCRIPTION = -1;

    public static int MAX_TIMELINE_LOADS = 3;

    public static ForkJoinPool DELETE_USER_FORK_JOIN_POOL = new ForkJoinPool(2);

}
