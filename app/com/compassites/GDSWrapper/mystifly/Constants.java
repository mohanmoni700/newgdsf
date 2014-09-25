package com.compassites.GDSWrapper.mystifly;

import java.util.HashMap;
import java.util.Map;

import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerTitle;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Gender;

import com.compassites.model.CabinClass;
import com.compassites.model.JourneyType;
import com.google.common.collect.ImmutableMap;

public class Constants {

	public static final String ALL = "All";
	public static final String DEFAULT = "Default";
	public static final String DIRECT = "Direct";
	public static final String ONE_STOP = "OneStop";

	public static final Map<JourneyType, String> JOURNEY_TYPE = ImmutableMap
			.of(JourneyType.ONE_WAY, "OneWay", JourneyType.ROUND_TRIP,
					"Return", JourneyType.MULTI_CITY, ""); // What goes here?

	private static final Map<CabinClass, String> cabinType;
	static {
		cabinType = new HashMap<CabinClass, String>();
		cabinType.put(CabinClass.ECONOMY, "Y");
		cabinType.put(CabinClass.PREMIUM_ECONOMY, "S");
		cabinType.put(CabinClass.BUSINESS, "C");
		cabinType.put(CabinClass.PREMIUM_BUSINESS, "J");
		cabinType.put(CabinClass.FIRST, "F");
		cabinType.put(CabinClass.PREMIUM_FIRST, "P");
	}

	public static final Map<CabinClass, String> CABIN_TYPE = ImmutableMap
			.copyOf(cabinType);

	public static final Map<String, Gender.Enum> GENDER = ImmutableMap.of(
			"male", Gender.M, "female", Gender.F);

	public static final Map<String, PassengerTitle.Enum> PASSENGER_TITLE = ImmutableMap
			.of("mr", PassengerTitle.MR, "mrs", PassengerTitle.MRS, "ms.",
					PassengerTitle.MISS, "master", PassengerTitle.MR, "miss",
					PassengerTitle.MISS);

}
