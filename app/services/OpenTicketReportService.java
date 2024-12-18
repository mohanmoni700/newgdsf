package services;

import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import dto.OpenTicketDTO;
import dto.OpenTicketResponse;

import java.util.List;

public interface OpenTicketReportService {
    public List<OpenTicketResponse> openTicketReport(List<OpenTicketDTO> ticketDTOList);
}
