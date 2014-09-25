package services;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Error;
import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.mystifly.AirRevalidateClient;
import com.compassites.GDSWrapper.mystifly.BookFlightClient;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;

/**
 * @author Santhosh
 */
@Service
public class MystiflyBookingServiceImpl implements BookingService {

	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		String fareSourceCode = travellerMasterInfo.getItinerary().getFareSourceCode();
		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		AirRevalidateRS revalidateRS = revalidateClient.revalidate(fareSourceCode);
		PNRResponse pnrRS = new PNRResponse();
		
		if(revalidateRS.getSuccess()) {
			BookFlightClient bookFlightClient = new BookFlightClient();
			AirBookRS airbookRS = bookFlightClient.bookFlight(travellerMasterInfo);
			if(airbookRS.getSuccess()) {
				pnrRS.setPnrNumber(airbookRS.getUniqueID());
				pnrRS.setFlightAvailable(airbookRS.getSuccess());
				pnrRS.setValidTillDate(airbookRS.getTktTimeLimit().toString());
			} else {
				ErrorMessage error = new ErrorMessage();
				Error[] errors = airbookRS.getErrors().getErrorArray();
				error.setErrorCode(errors[0].getCode());
				error.setMessage(errors[0].getMessage());
				error.setProvider("Mystifly");
				pnrRS.setErrorMessage(error);
			}
		//} else if(!revalidateRS.getIsValid()) {
			// New price
//		} else {
//			
//		}
		}
		return pnrRS;
	}

	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

}
