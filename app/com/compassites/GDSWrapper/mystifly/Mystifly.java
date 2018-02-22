package com.compassites.GDSWrapper.mystifly;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.CabinType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerTitle;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Gender;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target.Enum;

import com.compassites.model.CabinClass;
import com.compassites.model.JourneyType;
import com.compassites.model.PassengerTypeCode;
import com.google.common.collect.ImmutableMap;
import play.Play;

/**
 * @author Santhosh
 */
public class Mystifly {
	
	public static final Enum TARGET = Target.TEST;
	public static final String PROVIDER = "Mystifly";
	public static final String ENDPOINT_ADDRESS;
	public static final String ACCOUNT_NUMBER;
	public static final String USERNAME;
	public static final String PASSWORD;
	public static final int TIMEOUT = 180000;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-M-d'T'H:m:s");

	static {
		ENDPOINT_ADDRESS = Play.application().configuration().getString("mystifly.endPointURL");
		ACCOUNT_NUMBER = Play.application().configuration().getString("mystifly.accountNumber");
		USERNAME = Play.application().configuration().getString("mystifly.userName");
		PASSWORD = Play.application().configuration().getString("mystifly.password");
	}
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
			.of("mr.", PassengerTitle.MR, "mrs.", PassengerTitle.MRS, "ms.",
					PassengerTitle.MS, "mstr.", PassengerTitle.MSTR, "miss.",
					PassengerTitle.MISS);
	public static final Map<PassengerTypeCode, PassengerType.Enum> PASSENGER_TYPE = ImmutableMap
			.of(PassengerTypeCode.ADT, PassengerType.ADT,
					PassengerTypeCode.CHD, PassengerType.CHD,
					PassengerTypeCode.INF, PassengerType.INF);


	public static final String NO_ITINERARY_ERROR_CODE = "ERSER021";

}
