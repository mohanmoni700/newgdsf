package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yaseen on 12-05-2016.
 */
public class SystemUtility {

    static Logger logger = LoggerFactory.getLogger("gds");


    public static String getEnvironment() {
        String env = System.getenv("JOCEnv");
        logger.debug("Loading Environment configuration for :" + env);
        return env;
    }
}
