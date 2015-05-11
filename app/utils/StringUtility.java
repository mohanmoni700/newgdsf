package utils;

import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by user on 22-08-2014.
 */
public class StringUtility {

	public static String getPriceFromString(String price) {
		Matcher matcher = Pattern.compile("\\d+").matcher(price);
		matcher.find();
		int i = Integer.valueOf(matcher.start());
		String priceValue = price.substring(i);
		return priceValue;
	}

	public static BigDecimal getDecimalFromString(String str) {
		return new BigDecimal(str.replaceAll("[^\\d\\.]", ""));
	}
	
	public static String getCurrencyFromString(String str) {
		return str.replaceAll("[\\d\\.]", "");
	}

	public static String getGenderCode(String gender) {
		return "male".equalsIgnoreCase(gender) ? "M" : "F";
	}

    public static BigDecimal getLowestFareFromString(String numberText){
        String[] texts = numberText.split("\\s+");
        if(NumberUtils.isNumber(texts[texts.length - 2])){
            return new BigDecimal(texts[texts.length - 2]);
        }
        return null;
    }
}
