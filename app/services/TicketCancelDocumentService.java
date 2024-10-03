package services;

import com.amadeus.xml.trcanr_14_1_1a.TicketCancelDocumentReply;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.TicketCancelDocumentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

public interface TicketCancelDocumentService {
    public TicketCancelDocumentResponse ticketCancelDocument(String pnr, List<String> ticketsList);
}
