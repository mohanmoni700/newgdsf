package com.compassites.GDSWrapper.indigo;


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

import javax.xml.bind.JAXBElement;
import java.net.URL;
import java.util.Date;

@Service
public class SessionHandler {

    @Autowired
    private RedisTemplate redisTemplate;

    private ISessionManager iSessionManager;

    static Logger indigoLogger = LoggerFactory.getLogger("indigo");

    public SessionHandler() {
        try {
            SessionManager sessionManager = new SessionManager();
            iSessionManager = sessionManager.getBasicHttpBindingISessionManager();
            ((javax.xml.ws.BindingProvider) iSessionManager)
                    .getRequestContext()
                    .put(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://soapapir4y.test.6e.navitaire.com/SessionManager.svc");
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
/*            logonRequestData.setLocationCode(null);
            logonRequestData.setRoleCode(null);
            logonRequestData.setTerminalInfo(null);
            logonRequestData.setClientName(null);*/
            logonRequest.setLogonRequestData(factory.createLogonRequestData(logonRequestData));
            indigoLogger.debug("Indigo Session Req " + new Date() +" ---->" + new XStream().toXML(logonRequest));
            SessionManager sessionManager = new SessionManager();
            ISessionManager iSessionManager1 = sessionManager.getBasicHttpBindingISessionManager();
            ((javax.xml.ws.BindingProvider) iSessionManager1)
                    .getRequestContext()
                    .put(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://soapapir4y.test.6e.navitaire.com/SessionManager.svc");

            logonResponse = iSessionManager1.logon(logonRequest, Indigo.contractVersion, true);
            indigoLogger.debug("Indigo Session Response " + new Date() +" ---->" + logonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logonResponse;
    }

}
