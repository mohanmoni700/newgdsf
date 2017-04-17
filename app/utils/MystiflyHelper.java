package utils;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yaseen on 30-03-2016.
 */
public class MystiflyHelper {

    public static PricingInformation setPricingInformtions(
            AirItineraryPricingInfo airlinePricingInfo) {
        ItinTotalFare itinTotalFare = airlinePricingInfo.getItinTotalFare();
        PricingInformation pricingInfo = new PricingInformation();
        pricingInfo.setProvider(Mystifly.PROVIDER);
        pricingInfo
                .setLCC(airlinePricingInfo.getFareType() == FareType.WEB_FARE);
        pricingInfo.setCurrency(itinTotalFare.getBaseFare().getCurrencyCode());
        String baseFare = itinTotalFare.getEquivFare().getAmount();
        pricingInfo.setBasePrice(new BigDecimal(baseFare));
        String totalTax = itinTotalFare.getTotalTax().getAmount();
        pricingInfo.setTax(new BigDecimal(totalTax));
        String total = itinTotalFare.getTotalFare().getAmount();
        pricingInfo.setTotalPrice(new BigDecimal(total));
        pricingInfo.setGdsCurrency("INR");
        pricingInfo.setTotalPriceValue(pricingInfo.getTotalPrice());
        pricingInfo.setFareSourceCode(airlinePricingInfo.getFareSourceCode());
        setFareBeakup(pricingInfo, airlinePricingInfo);
        return pricingInfo;
    }

    public static void setFareBeakup(PricingInformation pricingInfo,
                               AirItineraryPricingInfo airlinePricingInfo) {
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (PTCFareBreakdown ptcFareBreakdown : airlinePricingInfo
                .getPTCFareBreakdowns().getPTCFareBreakdownArray()) {

            PassengerTax passengerTax = new PassengerTax();
            PassengerFare paxFare = ptcFareBreakdown.getPassengerFare();
            // passengerTax.setBaseFare(new BigDecimal(paxFare.getBaseFare()
            // .getAmount()));
            PassengerTypeQuantity passenger = ptcFareBreakdown
                    .getPassengerTypeQuantity();
            passengerTax.setPassengerCount(passenger.getQuantity());
            passengerTax.setPassengerType(passenger.getCode().toString());

            Map<String, BigDecimal> taxes = new HashMap<>();
            for (Tax tax : ptcFareBreakdown.getPassengerFare().getTaxes()
                    .getTaxArray()) {
                if (taxes.containsKey(tax.getTaxCode())) {
                    taxes.put(tax.getTaxCode(), taxes.get(tax.getTaxCode())
                            .add(new BigDecimal(tax.getAmount())));
                } else {
                    taxes.put(tax.getTaxCode(), new BigDecimal(tax.getAmount()));
                }
            }
            passengerTax.setTaxes(taxes);
            passengerTaxes.add(passengerTax);
            String paxCode = ptcFareBreakdown.getPassengerTypeQuantity()
                    .getCode().toString();
            BigDecimal amount = new BigDecimal(paxFare.getEquivFare()
                    .getAmount());
            BigDecimal totalAmount = new BigDecimal(paxFare.getTotalFare().getAmount());
            if (paxCode.equalsIgnoreCase("ADT")) {
                pricingInfo.setAdtBasePrice(amount);
                pricingInfo.setAdtTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("CHD")) {
                pricingInfo.setChdBasePrice(amount);
                pricingInfo.setChdTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("INF")) {
                pricingInfo.setInfBasePrice(amount);
                pricingInfo.setInfTotalPrice(totalAmount);
            }
        }
        pricingInfo.setPassengerTaxes(passengerTaxes);
    }
}
