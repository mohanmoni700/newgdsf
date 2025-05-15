package services;


import com.compassites.model.TicketCheckEligibilityRes;
import com.compassites.model.TicketProcessRefundRes;


import java.util.List;

public interface RefundService {

    TicketCheckEligibilityRes checkTicketEligibility(String gdspnr,String searchOfficeId, String ticketingOfficeId);

    TicketProcessRefundRes processFullRefund(String gdspnr,String searchOfficeId, String ticketingOfficeId);

    TicketCheckEligibilityRes checkPartRefundTicketEligibility(List<String> ticketList, String gdspnr,String searchOfficeId, String ticketingOfficeId);

    TicketProcessRefundRes processPartialRefund(List<String> ticketList,String gdspnr,String searchOfficeId, String ticketingOfficeId);

}
