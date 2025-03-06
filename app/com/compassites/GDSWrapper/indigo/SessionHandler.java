package com.compassites.GDSWrapper.indigo;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.navitaire.schemas.webservices.ISessionManager;
import com.navitaire.schemas.webservices.LogonResponse;
import com.navitaire.schemas.webservices.SessionManager;
import com.navitaire.schemas.webservices.datacontracts.session.LogonRequestData;
import com.navitaire.schemas.webservices.datacontracts.session.ObjectFactory;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.navitaire.schemas.webservices.servicecontracts.sessionservice.LogonRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

@Service
public class SessionHandler {

    @Autowired
    private RedisTemplate redisTemplate;

    private ISessionManager iSessionManager;

    static Logger indigoLogger = LoggerFactory.getLogger("indigo");
    public static String endPoint = null;
    public static URL wsdlUrl;

    static {
        try{
            endPoint = play.Play.application().configuration().getString("indigo.domain.endPoint");
        }catch (Exception e){
            indigoLogger.debug("Error in loading Amadeus URL : ", e);
        }
    }

    public SessionHandler() {
        try {
            wsdlUrl = SessionHandler.class.getResource("/META-INF/wsdl/indigo/SessionManager.wsdl");
            QName serviceName = new QName("http://schemas.navitaire.com/WebServices", "SessionManager");
            SessionManager sessionManager = new SessionManager(wsdlUrl,serviceName);
            iSessionManager = sessionManager.getBasicHttpBindingISessionManager();
            /*
            ((javax.xml.ws.BindingProvider) iSessionManager)
                    .getRequestContext()
                    .put(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://soapapir4y.test.6e.navitaire.com/SessionManager.svc");
            */
            HashMap httpHeaders = new HashMap();
            httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
            httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
            Map reqContext = ((BindingProvider) iSessionManager).getRequestContext();
            reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
            reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
            //reqContext.put(BindingProvider.SOAPACTION_USE_PROPERTY, true);
            //reqContext.put(BindingProvider.SOAPACTION_URI_PROPERTY, "LogonRequest");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LogonResponse login() {
        ObjectFactory factory = new ObjectFactory();
        LogonRequest logonRequest = new LogonRequest();
        LogonResponse logonResponse = null;
        try {
            LogonRequestData logonRequestData = new LogonRequestData();
            logonRequestData.setDomainCode(factory.createLogonRequestDataDomainCode(Indigo.DOMAIN_CODE));
            logonRequestData.setAgentName(factory.createLogonRequestDataAgentName(Indigo.AGENT_CODE));
            logonRequestData.setPassword(factory.createLogonRequestDataPassword(Indigo.AGENT_PASSWORD));
            /*logonRequestData.setLocationCode(null);
            logonRequestData.setRoleCode(null);
            logonRequestData.setTerminalInfo(null);
            logonRequestData.setClientName(null);*/
            logonRequest.setLogonRequestData(factory.createLogonRequestData(logonRequestData));
            indigoLogger.debug("Indigo Session Req " + new Date() +" ---->" + convertToSoapXml(logonRequest));
            System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
            System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
            System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

            logonResponse = this.iSessionManager.logon(logonRequest, Indigo.contractVersion, false);
            indigoLogger.debug("Indigo Session Response " + new Date() +" ---->" + logonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logonResponse;
    }

    public String convertToSoapXml(LogonRequest logonRequest) throws JAXBException {
        // Marshal the Java object to XML
        JAXBContext jaxbContext = JAXBContext.newInstance(LogonRequest.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter xmlWriter = new StringWriter();
        marshaller.marshal(logonRequest, xmlWriter);
        String bodyXml = xmlWriter.toString();

        // Wrap the XML body inside a SOAP envelope
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Header/>\n" +
                "    <soapenv:Body>\n" +
                bodyXml +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

}
