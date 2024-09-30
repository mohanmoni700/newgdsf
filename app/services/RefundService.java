package services;

import com.amadeus.xml.fatceq_13_1_1a.TicketCheckEligibility;
import com.compassites.model.TicketCheckEligibilityRes;
import com.compassites.model.TicketProcessRefundRes;

import java.util.LinkedList;
import java.util.List;

public interface RefundService {
    public TicketCheckEligibilityRes checkTicketEligibility(String gdspnr,String searchOfficeId);
    public TicketProcessRefundRes processFullRefund(String gdspnr,String searchOfficeId);
    public TicketCheckEligibilityRes checkPartRefundTicketEligibility(List<String> ticketList, String gdspnr,String searchOfficeId);
    public TicketProcessRefundRes processPartialRefund(List<String> ticketList,String gdspnr,String searchOfficeId);

}
