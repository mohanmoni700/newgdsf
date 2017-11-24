package utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by sathishkumarpalanisamy on 24/11/17.
 */
public class CorporateCodeHelper {

    public static String getAirlineCorporateCode(String airlineCode){

        Properties prop = new Properties();
        InputStream input = null;
        String fileName = "conf/corporateCode.properties";
        try {
            input = new FileInputStream(fileName);
            prop.load(input);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if(prop.getProperty(airlineCode) != null){
            return prop.getProperty(airlineCode);
        }
        return null;
    }
}
