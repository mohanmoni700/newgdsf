package utils;

import play.Configuration;
import play.Play;

/**
 * Created by sathishkumarpalanisamy on 24/11/17.
 */
public class CorporateCodeHelper {

    static Configuration configuration = Play.application().configuration();

    public static String getAirlineCorporateCode(String airlineCode){
        if(configuration.getString(airlineCode) != null){
            return configuration.getString(airlineCode);
        }
        return null;
    }
}
