package com.compassites.GDSWrapper.amadeus;

import models.AmadeusSessionWrapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.soap.MessageFactory;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;


public class AmadeusSOAPHeaderHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger logger = LoggerFactory.getLogger("gds");
    private static final String SESSION_WRAPPER = "amadeusSessionWrapper";

    private static final String amadeusEndPointURL = play.Play.application().configuration().getString("amadeus.endPointURL");
    private static final String amadeusOriginator = play.Play.application().configuration().getString("amadeus.ORIGINATOR");
    private static final String amadeusAgentDutyCode = play.Play.application().configuration().getString("amadeus.REFERENCE_IDENTIFIER");
    private static final String amadeusRequestorType = play.Play.application().configuration().getString("amadeus.ORIGINATOR_TYPE_CODE");
    private static final String posType = play.Play.application().configuration().getString("amadeus.POS_TYPE");
    private static final String amadeusPassword = play.Play.application().configuration().getString("amadeus.soap4Pswd");
    private static final String amadeusWsapId = play.Play.application().configuration().getString("amadeus.wsapId");


    private static final String finalUrl = amadeusEndPointURL+"/"+amadeusWsapId;


    @Override
    public Set<QName> getHeaders() {
        final QName securityHeader = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security", "wsse");
        final HashSet<QName> headers = new HashSet<>();
        headers.add(securityHeader);
        return headers;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outBoundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        logger.debug("Handling message, outbound: {}", outBoundProperty);

        if (outBoundProperty != null && outBoundProperty) {
            try {
                SOAPMessage soapMessage = context.getMessage();
                if (soapMessage == null) {
                    logger.warn("SOAPMessage is null, creating new message");
                    soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();
                }

                SOAPPart soapPart = soapMessage.getSOAPPart();
                SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
                soapEnvelope.setPrefix("soap");

                soapEnvelope.removeNamespaceDeclaration("S");
                soapEnvelope.removeNamespaceDeclaration("SOAP-ENV");
                soapEnvelope.removeAttribute("xmlns:SOAP-ENV");

                soapEnvelope.addNamespaceDeclaration("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                soapEnvelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
                soapEnvelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
                soapEnvelope.addNamespaceDeclaration("ses", "http://xml.amadeus.com/2010/06/Session_v3");

                SOAPHeader header = soapEnvelope.getHeader();
                if (header == null) {
                    header = soapEnvelope.addHeader();
                    header.setPrefix("soap");
                } else {
                    header.removeContents();
                    header.setPrefix("soap");
                }

                AmadeusSessionWrapper sessionWrapper = (AmadeusSessionWrapper) context.get(SESSION_WRAPPER);

                //Common headers added here
                addCommonHeaders(header, context);

                if (sessionWrapper != null && sessionWrapper.isStateful()) {
                    addStatefulHeaders(header, sessionWrapper);
                } else {
                    assert sessionWrapper != null;
                    addSecurityHeaders(header, sessionWrapper);
                }

                SOAPBody body = soapEnvelope.getBody();
                if (body != null) {
                    body.setPrefix("soap");
                    Iterator<?> children = body.getChildElements();
                    SOAPElement correctBodyContent = null;
                    while (children.hasNext()) {
                        Object child = children.next();
                        if (child instanceof SOAPElement) {
                            SOAPElement element = (SOAPElement) child;
                            if ("Body".equals(element.getLocalName())) {
                                Iterator<?> nestedChildren = element.getChildElements();
                                while (nestedChildren.hasNext()) {
                                    Object nestedChild = nestedChildren.next();
                                    if (nestedChild instanceof SOAPElement) {
                                        correctBodyContent = (SOAPElement) nestedChild;
                                        body.addChildElement(correctBodyContent);
                                    }
                                }
                                element.detachNode();
                            } else {
                                correctBodyContent = element;
                            }
                        }
                    }
                    if (correctBodyContent != null) {
                        stripAllNamespaces(correctBodyContent);
                    }
                }

                soapMessage.saveChanges();
                logger.debug("SOAP Message constructed successfully");

            } catch (Exception e) {
                logger.debug("Error Creating SOAP Message for Amadeus Request  {} : ", e.getMessage(), e);
                e.printStackTrace();
            }
        }
        return true;
    }

    private void addStatefulHeaders(SOAPHeader header, AmadeusSessionWrapper sessionWrapper) throws SOAPException {

        SOAPElement mainSessionHeader = header.addChildElement("Session", "ses");

        String transactionStatus;
        int sequenceNum = Integer.parseInt(sessionWrapper.getSequenceNumber() != null ? sessionWrapper.getSequenceNumber() : "0");

        if (sessionWrapper.isLogout()) {
            transactionStatus = "End";
        } else {
            transactionStatus = (sequenceNum == 1) ? "Start" : "InSeries";
        }
        mainSessionHeader.addAttribute(QName.valueOf("TransactionStatusCode"), transactionStatus);

        if ("Start".equals(transactionStatus)) {

            addSecurityHeaders(header, sessionWrapper);

        } else {

            String sessionId = sessionWrapper.getSessionId() != null ? sessionWrapper.getSessionId() : "";
            String sequenceNumber = sessionWrapper.getSequenceNumber() != null ? sessionWrapper.getSequenceNumber() : "0";
            String securityToken = sessionWrapper.getSecurityToken() != null ? sessionWrapper.getSecurityToken() : "";

            mainSessionHeader.addChildElement("SessionId", "ses").addTextNode(sessionId);
            mainSessionHeader.addChildElement("SequenceNumber", "ses").addTextNode(sequenceNumber);
            mainSessionHeader.addChildElement("SecurityToken", "ses").addTextNode(securityToken);

        }

    }

    // Common Headers for both stateful and stateless are added here
    private void addCommonHeaders(SOAPHeader header, SOAPMessageContext context) throws SOAPException {

        // Message ID set here
        SOAPElement messageID = header.addChildElement("MessageID", "add", "http://www.w3.org/2005/08/addressing");
        messageID.addTextNode(UUID.randomUUID().toString());

        // Action set here
        SOAPElement action = header.addChildElement("Action", "add", "http://www.w3.org/2005/08/addressing");
        String actionValue = (String) context.get("javax.xml.ws.soap.http.soapaction.uri");
        action.addTextNode(actionValue != null ? actionValue : "");

        // To
        SOAPElement to = header.addChildElement("To", "add", "http://www.w3.org/2005/08/addressing");
        to.addTextNode(finalUrl);

        // Transaction Flow link
        header.addChildElement("TransactionFlowLink", "link", "http://wsdl.amadeus.com/2010/06/ws/Link_v1");

    }


    //Stateless / Security headers are added here
    private void addSecurityHeaders(SOAPHeader header, AmadeusSessionWrapper sessionWrapper) throws SOAPException {

        SOAPElement securityElement = header.addChildElement("Security", "oas", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        addUserNameTokenElement(securityElement);

        addAMASecurityUser(header, sessionWrapper.getOfficeId());

    }


    private void addUserNameTokenElement(SOAPElement securityElement) throws SOAPException {

        SOAPElement userNameToken = securityElement.addChildElement("UsernameToken", "oas");

        userNameToken.addNamespaceDeclaration("oas1", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        userNameToken.addAttribute(new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id", "oas1"), "UsernameToken-1");

        SOAPElement userName = userNameToken.addChildElement("Username", "oas");
        userName.addTextNode(amadeusOriginator);

        String nonceValue = RandomStringUtils.random(11, true, true);
        String encodedNonceValue = Base64.encodeBase64String(nonceValue.getBytes(StandardCharsets.UTF_8));
        String timeStamp = Instant.now().toString();

        SOAPElement nonce = userNameToken.addChildElement("Nonce", "oas");
        nonce.addAttribute(QName.valueOf("EncodingType"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
        nonce.addTextNode(encodedNonceValue);

        SOAPElement password = userNameToken.addChildElement("Password", "oas");
        password.addAttribute(new QName("Type"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");
        password.addTextNode(buildPasswordDigest(amadeusPassword, nonceValue, timeStamp));

        SOAPElement created = userNameToken.addChildElement("Created", "oas1");
        created.addTextNode(timeStamp);

    }


    private void stripAllNamespaces(SOAPElement element) {
        element.setPrefix("");

        Iterator<?> prefixes = element.getNamespacePrefixes();
        List<String> prefixesToRemove = new ArrayList<>();
        while (prefixes.hasNext()) {
            String prefix = (String) prefixes.next();
            prefixesToRemove.add(prefix);
        }
        for (String prefix : prefixesToRemove) {
            element.removeNamespaceDeclaration(prefix);
        }

        Iterator<?> attributes = element.getAllAttributes();
        while (attributes.hasNext()) {
            Name attribute = (Name) attributes.next();
            if (attribute.getLocalName().startsWith("xmlns")) {
                element.removeAttribute(attribute);
            }
        }

        NamedNodeMap namedNodeMap = element.getAttributes();
        String nodeName = String.valueOf(namedNodeMap.getNamedItem("xmlns"));
        if (nodeName != null && (nodeName.equalsIgnoreCase("xmlns=\"http://www.iata.org/IATA/2007/00/IATA2010.1\"")
                || nodeName.equalsIgnoreCase("xmlns=\"http://xml.amadeus.com/2010/06/Types_v2\""))) {
            namedNodeMap.removeNamedItem("xmlns");
        }

        Iterator<?> children = element.getChildElements();
        while (children.hasNext()) {
            Object child = children.next();
            if (child instanceof SOAPElement) {
                stripAllNamespaces((SOAPElement) child);
            }
        }
    }

    public static String buildPasswordDigest(String password, String nonce, String dateTime) {

        MessageDigest sha1;
        String passwordDigest = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(password.getBytes(StandardCharsets.UTF_8));
            sha1.update(nonce.getBytes(StandardCharsets.UTF_8));
            sha1.update(dateTime.getBytes(StandardCharsets.UTF_8));
            passwordDigest = Base64.encodeBase64String(sha1.digest(hash));
            sha1.reset();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error in buildPasswordDigest", e);
        }
        return passwordDigest;
    }

    private void addAMASecurityUser(SOAPElement headerElement, String officeId) throws SOAPException {

        SOAPElement amaSecurityUser = headerElement.addChildElement("AMA_SecurityHostedUser", "", "http://xml.amadeus.com/2010/06/Security_v1");
        SOAPElement userId = amaSecurityUser.addChildElement("UserID");
        userId.setAttribute("AgentDutyCode", amadeusAgentDutyCode);
        userId.setAttribute("POS_Type", posType);
        userId.setAttribute("RequestorType", amadeusRequestorType);

        userId.setAttribute("PseudoCityCode", officeId);

    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return false;
    }

    @Override
    public void close(MessageContext context) {
    }
}