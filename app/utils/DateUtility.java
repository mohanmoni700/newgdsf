package utils;

import com.compassites.model.PassengerTypeCode;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtility {

	public static PassengerTypeCode getPassengerTypeFromDOB(Date dob) {
        if(dob==null)
            return PassengerTypeCode.ADT;
        long arr[] = getAgeFromDOB(dob);
        long yearDiff = arr[0];
        long monthDiff = arr[1];
        long dateDiff = arr[2];
        if (yearDiff < 2) {
            return PassengerTypeCode.INF;
        } else if (yearDiff == 2) {
            if (monthDiff > 0 || dateDiff > 0) {
                return PassengerTypeCode.CHD;
            } else {
                return PassengerTypeCode.INF;
            }
        } else if (yearDiff < 12) {
            return PassengerTypeCode.CHD;
        } else if (yearDiff == 12) {
            if (monthDiff > 0 || dateDiff > 0) {
                return PassengerTypeCode.ADT;
            } else {
                return PassengerTypeCode.CHD;
            }
        } else {
            return PassengerTypeCode.ADT;
        }
	}

    public static long[]  getAgeFromDOB(Date dob) {
        LocalDate birthDate = new LocalDate(dob);
        LocalDate now = new LocalDate();
        Period period = new Period(birthDate, now, PeriodType.yearMonthDay());
        long arr[] = {period.getYears(), period.getMonths(), period.getDays()};
        return arr;
    }

    public static long  getAgeFromDOBRounded(Date dob) {
        LocalDate birthDate = new LocalDate(dob);
        LocalDate now = new LocalDate();
        Period period = new Period(birthDate, now, PeriodType.yearMonthDay());
        return period.getYears();
    }


    public static Duration convertToDuration(String totalElapsedTime){
        String strHours = totalElapsedTime.substring(0, 2);
        String strMinutes = totalElapsedTime.substring(2);
        Duration duration = null;
        Integer hours = new Integer(strHours);
        int days = hours / 24;
        int dayHours = hours - (days * 24);
        try {
            duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, days, dayHours, new Integer(strMinutes), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return duration;
    }

    public static DateTime convertTimewithZone(String zone,String date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern(dateFormat);
        DateTimeZone dateTimeZone = DateTimeZone.forID(zone);
        DateTime convertedDate = dateTimeFormat.withZone(dateTimeZone).parseDateTime(date);
       return convertedDate;
    }

    public static Duration convertMillistoString(long timeinmillisec){
        Duration duration = null;
        Long days = TimeUnit.MILLISECONDS.toDays(timeinmillisec);
        Long dayHours = TimeUnit.MILLISECONDS.toHours(timeinmillisec);
        Long minutes = TimeUnit.MILLISECONDS.toMinutes(timeinmillisec) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeinmillisec));
        Long seconds = TimeUnit.MILLISECONDS.toSeconds(timeinmillisec) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeinmillisec));
       try {
            duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, days.intValue(), dayHours.intValue(),minutes.intValue(), seconds.intValue());
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return duration;
    }



}
