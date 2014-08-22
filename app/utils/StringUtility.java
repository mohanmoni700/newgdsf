package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 22-08-2014.
 */
public class StringUtility {

    public static String getPriceFromString(String price){
        Matcher matcher = Pattern.compile("\\d+").matcher(price);
        matcher.find();
        int i = Integer.valueOf(matcher.start());
        String priceValue = price.substring(i);
        return priceValue;
    }
}
