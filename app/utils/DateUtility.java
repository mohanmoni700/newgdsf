package utils;

import com.compassites.model.PassengerTypeCode;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.Date;

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
}
