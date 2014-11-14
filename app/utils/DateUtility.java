package utils;

import com.compassites.model.PassengerTypeCode;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.Date;

public class DateUtility {

	public static PassengerTypeCode getPassengerTypeFromDOB(Date dob) {
		LocalDate birthDate = new LocalDate(dob);
		LocalDate now = new LocalDate();
		Period period = new Period(birthDate, now, PeriodType.yearMonthDay());
		int age = period.getYears();
		return age <= 2 ? PassengerTypeCode.INF
				: (age <= 12 ? PassengerTypeCode.CHD : PassengerTypeCode.ADT);
	}


    public static long  getAgeFromDOB(Date dob){
        LocalDate birthDate = new LocalDate(dob);
        LocalDate now = new LocalDate();
        Period period = new Period(birthDate, now, PeriodType.yearMonthDay());
        long age = period.getYears();
        return age;
    }

}
