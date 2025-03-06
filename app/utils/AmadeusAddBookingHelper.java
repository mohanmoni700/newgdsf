package utils;

import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.model.PassengerTax;
import com.compassites.model.TSTPrice;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmadeusAddBookingHelper {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");
    public static PassengerTax getTaxDetailsFromTST(List<TicketDisplayTSTReply.FareList.TaxInformation> taxInformationList, String passengerType, int count){
        PassengerTax passengerTax = new PassengerTax();
        passengerTax.setPassengerType(passengerType);
        passengerTax.setPassengerCount(count);
        Map<String, BigDecimal> taxes = new HashMap<>();
        for(TicketDisplayTSTReply.FareList.TaxInformation taxInformation : taxInformationList){
            String amount = taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount();
            String taxCode = taxInformation.getTaxDetails().getTaxType().getIsoCountry();
            if(taxes.containsKey(taxCode)) {
                taxes.put(taxCode, taxes.get(taxCode).add(new BigDecimal(amount)));
            } else {
                taxes.put(taxCode, new BigDecimal(amount));
            }
        }

        passengerTax.setTaxes(taxes);

        return passengerTax;
    }

    public static TSTPrice getTSTPrice(TicketDisplayTSTReply.FareList fare, BigDecimal paxTotalFare, BigDecimal paxBaseFare, String paxType, PassengerTax passengerTax){
        TSTPrice tstPrice = new TSTPrice();
        for(TicketDisplayTSTReply.FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
            com.amadeus.xml.ttstrr_13_1_1a.BaggageDetailsTypeI bagAllowance =  null;
            if(segmentInfo.getBagAllowanceInformation()!=null){
                bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
                if(bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                    if(tstPrice.getMaxBaggageWeight() == 0 || tstPrice.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                        tstPrice.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                    }
                } else {
                    if(tstPrice.getBaggageCount() == 0 || tstPrice.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                        tstPrice.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                    }
                }
            }

            //reading booking class(RBD)
            String bookingClass = null;
            if(segmentInfo.getSegDetails()!=null && segmentInfo.getSegDetails().getSegmentDetail()!=null){
                bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
            }
            tstPrice.setBookingClass(bookingClass);
        }

        tstPrice.setTotalPrice(paxTotalFare);
        tstPrice.setBasePrice(paxBaseFare);
        tstPrice.setPassengerType(paxType);
        tstPrice.setPassengerTax(passengerTax);
        return tstPrice;
    }
}
