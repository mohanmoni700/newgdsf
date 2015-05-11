package utils;

import com.compassites.constants.HoldTime;
import com.compassites.model.Journey;
import com.compassites.model.traveller.TravellerMasterInfo;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by ritesh on 5/9/15.
 */
public class HoldTimeUtility {
    public static Calendar getCalendar(Date date,int daysToAdd){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(daysToAdd == 0){
            return calendar;
        }else{
            calendar.add(Calendar.DATE,daysToAdd);
            return calendar;
        }
    }
    public static Date getTime(int hours){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY,hours);
        return calendar.getTime();
    }
    public static Date getHoldTime(TravellerMasterInfo travellerMasterInfo){
        List<Journey> journeyList = null;
        if(travellerMasterInfo.isSeamen()) {
        	journeyList = travellerMasterInfo.getItinerary().getJourneyList();
        } else {
        	journeyList = travellerMasterInfo.getItinerary().getNonSeamenJourneyList();
        }
        Date travelDate = journeyList.get(0).getAirSegmentList().get(0).getDepartureDate();
        Calendar calendar = getCalendar(travelDate,0);

        if(calendar.equals(getCalendar(new Date(),0)) || (calendar.after(getCalendar(new Date(),0)) && calendar.before(getCalendar(new Date(),2)))){
            HoldTime holdTime =HoldTime.ZERO_TO_ONE;
            return getTime(holdTime.getHoldTime());
        }else if(calendar.after(getCalendar(new Date(),1)) && calendar.before(getCalendar(new Date(),6))){
            HoldTime holdTime = HoldTime.TWO_TO_FIVE;
            return getTime(holdTime.getHoldTime());
        }else if(calendar.after(getCalendar(new Date(),5)) && calendar.before(getCalendar(new Date(),11))){
            HoldTime holdTime = HoldTime.SIX_TO_TEN;
            return getTime(holdTime.getHoldTime());
        }else if(calendar.after(getCalendar(new Date(),10)) && calendar.before(getCalendar(new Date(),21))){
            HoldTime holdTime = HoldTime.ELEVEN_TO_TWENTY;
            return getTime(holdTime.getHoldTime());
        }else {
            HoldTime holdTime = HoldTime.TWENTY_ONE_TO_THIRTY;
            return getTime(holdTime.getHoldTime());
        }
    }
}
