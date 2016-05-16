package utils;

/**
 * Created by yaseen on 12-05-2016.
 */
public class SystemUtility {

    public static String getEnvironment() {
        String env = System.getenv("JOCEnv");

        return env;
    }
}
