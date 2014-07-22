package utils;

import com.compassites.model.ErrorMessage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by user on 22-07-2014.
 */
public class ErrorMessageHelper {

    public static ErrorMessage createErrorMessage(String errorCode,ErrorMessage.ErrorType type,String provider){
        Properties prop = new Properties();
        InputStream input = null;
        String fileName = "conf/errorCodes.properties";
        try {
            input = new FileInputStream(fileName);
            prop.load(input);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if(!prop.containsKey(errorCode)){
            errorCode = "error";
        }
        ErrorMessage errMessage = new ErrorMessage();
        errMessage.setMessage(prop.getProperty(errorCode));
        errMessage.setType(type);
        errMessage.setProvider(provider);
        return errMessage;
    }

    public static boolean checkErrorCodeExist(String errorCode){

        Properties prop = new Properties();
        InputStream input = null;
        String fileName = "conf/repeatErrorCodes.properties";
        try {
            input = new FileInputStream(fileName);
            prop.load(input);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if(prop.contains(errorCode)){
            return true;
        }
        return false;
    }
}
