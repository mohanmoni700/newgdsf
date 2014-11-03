package com.compassites.GDSWrapper.mystifly;

import java.util.HashMap;
import java.util.Map;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.CabinType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerTitle;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Gender;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target.Enum;

import com.compassites.model.CabinClass;
import com.compassites.model.JourneyType;
import com.google.common.collect.ImmutableMap;

/**
 * @author Santhosh
 */
public class Mystifly {
	
	public static final Enum TARGET = Target.TEST;
	public static final String PROVIDER = "Mystifly";
	public static final String ENDPOINT_ADDRESS = "http://apidemo.myfarebox.com/V2/OnePoint.svc?singleWsdl";
	public static final String ACCOUNT_NUMBER = "MCN004030";
	public static final String USERNAME = "FlyHiXML";
	public static final String PASSWORD = "FH2014_xml";
	public static final int TIMEOUT = 180000;

	private static Map<CabinClass, CabinType.Enum> cabinType = new HashMap<>();
	static {
		cabinType.put(CabinClass.ECONOMY, CabinType.Y);
		cabinType.put(CabinClass.PREMIUM_ECONOMY, CabinType.S);
		cabinType.put(CabinClass.BUSINESS, CabinType.C);
		cabinType.put(CabinClass.PREMIUM_BUSINESS, CabinType.J);
		cabinType.put(CabinClass.FIRST, CabinType.F);
		cabinType.put(CabinClass.PREMIUM_FIRST, CabinType.P);
	}

	public static final Map<JourneyType, AirTripType.Enum> JOURNEY_TYPE = ImmutableMap
			.of(JourneyType.ONE_WAY, AirTripType.ONE_WAY,
					JourneyType.ROUND_TRIP, AirTripType.RETURN,
					JourneyType.MULTI_CITY, AirTripType.OPEN_JAW);
	public static final Map<CabinClass, CabinType.Enum> CABIN_TYPE = ImmutableMap
			.copyOf(cabinType);
	public static final Map<String, Gender.Enum> GENDER = ImmutableMap.of(
			"male", Gender.M, "female", Gender.F);
	public static final Map<String, PassengerTitle.Enum> PASSENGER_TITLE = ImmutableMap
			.of("mr", PassengerTitle.MR, "mrs", PassengerTitle.MRS, "ms.",
					PassengerTitle.MISS, "master", PassengerTitle.MR, "miss",
					PassengerTitle.MISS);

}
