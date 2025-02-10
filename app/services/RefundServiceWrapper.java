package services;

import com.compassites.model.TicketCheckEligibilityRes;
import com.compassites.model.TicketProcessRefundRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class RefundServiceWrapper {

    @Autowired
    public AmadeusRefundServiceImpl amadeusRefundService;

    public TicketCheckEligibilityRes checkTicketEligibility(String provider, String gdsPNR,String searchOfficeId){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;
      if(provider.equalsIgnoreCase("Amadeus")){
          ticketCheckEligibilityRes =  amadeusRefundService.checkTicketEligibility(gdsPNR,searchOfficeId);
      }
      return ticketCheckEligibilityRes;
    }

    public TicketProcessRefundRes processFullRefund(String provider, String gdsPNR,String searchOfficeId){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        if(provider.equalsIgnoreCase("Amadeus")){
            ticketProcessRefundRes =  amadeusRefundService.processFullRefund(gdsPNR,searchOfficeId);
        }
        return ticketProcessRefundRes;
    }

    public TicketCheckEligibilityRes checkPartRefundTicketEligibility(String provider, String gdsPNR, List<String> ticketList,String searchOfficeId){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;

        if(provider.equalsIgnoreCase("Amadeus")){
            ticketCheckEligibilityRes =  amadeusRefundService.checkPartRefundTicketEligibility(ticketList,gdsPNR,searchOfficeId);
        }
        return ticketCheckEligibilityRes;
    }

    public TicketProcessRefundRes processPartialRefund(String provider, String gdsPNR,List<String> ticketList,String searchOfficeId){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        if(provider.equalsIgnoreCase("Amadeus")){
            ticketProcessRefundRes =  amadeusRefundService.processPartialRefund(ticketList,gdsPNR,searchOfficeId);
        }
        return ticketProcessRefundRes;
    }


}
