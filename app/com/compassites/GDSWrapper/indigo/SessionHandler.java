package com.compassites.GDSWrapper.indigo;

import com.compassites.constants.CacheConstants;
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
import javax.xml.ws.WebServiceFeature;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;

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
            SessionManager service = new SessionManager(wsdlUrl);
            WebServiceFeature[] features = {
                    new AddressingFeature(true), // Enables WS-Addressing
                    new MTOMFeature(false)        // Enables MTOM
            };
            iSessionManager = service.getBasicHttpBindingISessionManager();
            ((BindingProvider) iSessionManager).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LogonResponse login() {
        LogonResponse logonResponse = null;
        try {
            ObjectFactory factory = new ObjectFactory();
            com.navitaire.schemas.webservices.servicecontracts.sessionservice.ObjectFactory factory1 = new com.navitaire.schemas.webservices.servicecontracts.sessionservice.ObjectFactory();
            LogonRequestData requestData = factory.createLogonRequestData();
            LogonRequest logonRequest = factory1.createLogonRequest();
            requestData.setDomainCode(factory.createLogonRequestDataDomainCode(Indigo.DOMAIN_CODE));
            requestData.setAgentName(factory.createLogonRequestDataAgentName(Indigo.AGENT_CODE));
            requestData.setPassword(factory.createLogonRequestDataPassword(Indigo.AGENT_PASSWORD));
            logonRequest.setLogonRequestData(factory1.createLogonRequestLogonRequestData(requestData));
            indigoLogger.debug("Indigo Session Req " + new Date() +" ---->" + convertToSoapXml(logonRequest));
            logonResponse = this.iSessionManager.logon(logonRequest, Indigo.contractVersion, false);
            indigoLogger.debug("Indigo Session Response " + new Date() +" ---->" + convertToSoapXml(logonResponse));
            if(logonResponse.getSignature().getValue()!=null) {
                redisTemplate.opsForValue().set(Indigo.sessionValidity, logonResponse.getSignature().getValue());
                redisTemplate.expire(Indigo.sessionValidity, CacheConstants.INDIGO_CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logonResponse;
    }

    public String convertToSoapXml(Object object) throws JAXBException {
        if (object == null) {
            throw new IllegalArgumentException("Object to convert cannot be null");
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter xmlWriter = new StringWriter();
        marshaller.marshal(object, xmlWriter);
        return xmlWriter.toString();
    }

}
