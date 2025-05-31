package services;

import com.compassites.model.TicketCheckEligibilityRes;
import com.compassites.model.TicketProcessRefundRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class RefundServiceWrapper {

    @Autowired
    public RefundService amadeusRefundService;

    public TicketCheckEligibilityRes checkTicketEligibility(String provider, String gdsPNR,String searchOfficeId, String ticketingOfficeId){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;
      if(provider.equalsIgnoreCase("Amadeus")){
          ticketCheckEligibilityRes =  amadeusRefundService.checkTicketEligibility(gdsPNR,searchOfficeId,ticketingOfficeId);
      }
      return ticketCheckEligibilityRes;
    }

    public TicketProcessRefundRes processFullRefund(String provider, String gdsPNR,String searchOfficeId, String ticketingOfficeId){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        if(provider.equalsIgnoreCase("Amadeus")){
            ticketProcessRefundRes =  amadeusRefundService.processFullRefund(gdsPNR,searchOfficeId, ticketingOfficeId);
        }
        return ticketProcessRefundRes;
    }

    public TicketCheckEligibilityRes checkPartRefundTicketEligibility(String provider, String gdsPNR, List<String> ticketList,String searchOfficeId, String ticketingOfficeId){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;

        if(provider.equalsIgnoreCase("Amadeus")){
            ticketCheckEligibilityRes =  amadeusRefundService.checkPartRefundTicketEligibility(ticketList,gdsPNR,searchOfficeId, ticketingOfficeId);
        }
        return ticketCheckEligibilityRes;
    }

    public TicketProcessRefundRes processPartialRefund(String provider, String gdsPNR,List<String> ticketList,String searchOfficeId, String ticketingOfficeId){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        if(provider.equalsIgnoreCase("Amadeus")){
            ticketProcessRefundRes =  amadeusRefundService.processPartialRefund(ticketList,gdsPNR,searchOfficeId, ticketingOfficeId);
        }
        return ticketProcessRefundRes;
    }


}
