package services;


import com.compassites.model.TicketCancelDocumentResponse;


import java.util.List;

public interface TicketCancelDocumentService {
    TicketCancelDocumentResponse ticketCancelDocument(String pnr, List<String> ticketsList, String ticketingOfficeId);
}
