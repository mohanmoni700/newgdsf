package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.servicebookandprice_v1_v4.ServiceBookAndPricePT;
import com.amadeus.wsdl.servicebookandprice_v1_v4.ServiceBookAndPriceService;
import com.amadeus.xml._2010._06.servicebookandprice_v1.AMAServiceBookPriceServiceRQ;
import com.amadeus.xml._2010._06.servicebookandprice_v1.AMAServiceBookPriceServiceRS;
import com.thoughtworks.xstream.XStream;
import dto.AncillaryConfirmPaymentRQ;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.net.URL;
import java.util.*;

@Service
public class AncillaryPaymentConfirmation {

    ServiceBookAndPricePT serviceBookAndPricePT;

    public static URL wsdlUrl;

    private final BindingProvider bindingProvider;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");

    public static String endPoint = null;

    static {
        URL url = null;
        try {
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = AncillaryPaymentConfirmation.class.getResource("/META-INF/wsdl/amadeus4/1ASIWFLYFYH_PDT_ServiceBookAndPrice_1.2_4.0.wsdl");
        } catch (Exception e) {
            logger.debug("Error in loading ServiceBookAndPricePT URL : {} \n ", e.getMessage(), e);
        }
        wsdlUrl = url;
    }

    public AncillaryPaymentConfirmation() throws Exception {

        ServiceBookAndPriceService service = new ServiceBookAndPriceService(wsdlUrl);
        serviceBookAndPricePT = service.getServiceBookAndPricePort();
        bindingProvider = (BindingProvider) serviceBookAndPricePT;

        HashMap<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put("Accept", Collections.singletonList("text/xml, multipart/related"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));

        Map<String, Object> reqContext = bindingProvider.getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);

        List<Handler> handlerChain = new ArrayList<>();
        handlerChain.add(new AmadeusSOAPHeaderHandler());
        bindingProvider.getBinding().setHandlerChain(handlerChain);

    }

    public AMAServiceBookPriceServiceRS bookAndPriceAncillary(AmadeusSessionWrapper amadeusSessionWrapper, AncillaryConfirmPaymentRQ ancillaryConfirmPaymentRQ) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        AMAServiceBookPriceServiceRQ serviceBookPriceServiceRQ = AncillaryServiceReq.AdditionalPaidBaggage.getAncillaryPaymentConfirmationRequest(ancillaryConfirmPaymentRQ);
        amadeusLogger.debug("Ancillary Payment Confirmation AMAServiceBookPriceServiceRQ Request Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceBookPriceServiceRQ));
        AMAServiceBookPriceServiceRS amaServiceBookPriceServiceRS = serviceBookAndPricePT.serviceBookPriceService(serviceBookPriceServiceRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Ancillary Payment Confirmation AMAServiceBookPriceServiceRS Response Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaServiceBookPriceServiceRS));

        return amaServiceBookPriceServiceRS;
    }


}