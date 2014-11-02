package utils;

import java.util.Date;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.compassites.model.PassengerTypeCode;

public class DateUtility {

	public static PassengerTypeCode getPassengerTypeFromDOB(Date dob) {
		LocalDate birthdate = new LocalDate(dob);
		LocalDate now = new LocalDate();
		Period period = new Period(birthdate, now, PeriodType.yearMonthDay());
		int age = period.getYears();
		return age <= 2 ? PassengerTypeCode.INF
				: (age <= 12 ? PassengerTypeCode.CHD : PassengerTypeCode.ADT);
	}

}
