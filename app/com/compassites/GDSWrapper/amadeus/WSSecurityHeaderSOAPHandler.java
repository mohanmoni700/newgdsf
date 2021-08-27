package com.compassites.GDSWrapper.amadeus;

import com.google.common.hash.Hashing;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import utils.EncryptionUtil;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static utils.EncryptionUtil.*;
import static utils.EncryptionUtil.encodeToSHA1;

public class WSSecurityHeaderSOAPHandler implements SOAPHandler<SOAPMessageContext> {
    @Override
    public Set<QName> getHeaders() {
        final QName securityHeader = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "Security", "wsse");
        final HashSet headers = new HashSet();
        headers.add(securityHeader);

        return headers;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        Boolean outBoundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if(outBoundProperty){
            SOAPMessage soapMessage = context.getMessage();
            try {
                SOAPEnvelope soapEnvelope = context.getMessage().getSOAPPart().getEnvelope();
                soapEnvelope.getHeader().removeContents();

                SOAPHeader header = soapEnvelope.getHeader();
//                header.setAttribute("xmlns:add", "http://www.w3.org/2005/08/addressing");

                SOAPElement messageID = header.addChildElement("MessageID", "add", "http://www.w3.org/2005/08/addressing");
//                messageID.setAttribute("xmlns:add", "http://www.w3.org/2005/08/addressing");
                messageID.addTextNode(UUID.randomUUID().toString());

                SOAPElement to = header.addChildElement("To",  "add", "http://www.w3.org/2005/08/addressing");
//                messageID.setAttribute("xmlns:add", "http://www.w3.org/2005/08/addressing");
                to.addTextNode("https://nodeD1.test.webservices.amadeus.com/1ASIWFLYFYH");

                SOAPElement action = header.addChildElement("Action",  "add", "http://www.w3.org/2005/08/addressing");
//                messageID.setAttribute("xmlns:add", "http://www.w3.org/2005/08/addressing");
                action.addTextNode("http://webservices.amadeus.com/FMPTBQ_14_2_1A");


                SOAPElement securityElement = header.addChildElement("Security", "oas", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

                addUserNameTokenElement(securityElement);
                addAMASecurityUser(header);

            } catch (SOAPException e) {
                e.printStackTrace();
            }
        }
        return outBoundProperty;
    }


    private void addUserNameTokenElement(SOAPElement securityElement) throws SOAPException {
        SOAPElement userNameToken = securityElement.addChildElement("UsernameToken", "oas");
        userNameToken.addAttribute(new QName("xmlns:oas1"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

        SOAPElement userName = userNameToken.addChildElement("Username", "oas");
        userName.addTextNode("WSFYHFLY");

        String nonceValue = RandomStringUtils.random(11, true, true);
        String encodedNonceValue = Base64.encodeBase64String(nonceValue.getBytes());
        String timeStamp = Instant.now().toString();


        SOAPElement password = userNameToken.addChildElement("Password", "oas");
        password.addAttribute(new QName("Type"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username- token-profile-1.0#PasswordDigest");
        password.addTextNode(buildPasswordDigest("AMADEUS100",nonceValue, timeStamp));

        SOAPElement nonce = userNameToken.addChildElement("Nonce", "oas");
        nonce.addAttribute(QName.valueOf("EncodingType"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap- message-security-1.0#Base64Binary");
        nonce.addTextNode(encodedNonceValue);

        SOAPElement created = userNameToken.addChildElement("Created", "oas1");
        created.addTextNode(timeStamp);

    }

    private String getPassword(String nonce, String timestamp){
        String password = "";
        String s1 = encodeToSHA1(nonce + timestamp + encodeToSHA1("AMADEUS"));
        System.out.println("s1: " + s1);
        password = Base64.encodeBase64String(s1.getBytes());

        return password;
    }


    public static String buildPasswordDigest(String password, String nonce, String dateTime) {
        MessageDigest sha1;
        String passwordDigest = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(password.getBytes("UTF-8"));
            sha1.update(nonce.getBytes("UTF-8"));
            sha1.update(dateTime.getBytes("UTF-8"));
            passwordDigest = new String(Base64.encodeBase64String(sha1.digest(hash)));
            sha1.reset();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return passwordDigest;
    }

    private void addAMASecurityUser(SOAPElement headerElement) throws SOAPException {

        SOAPElement amaSecurityUser = headerElement.addChildElement("AMA_SecurityHostedUser", "", "http://xml.amadeus.com/2010/06/Security_v1");
//        amaSecurityUser.setAttribute("xmlns", "http://xml.amadeus.com/2010/06/Security_v1");

        SOAPElement userId = amaSecurityUser.addChildElement("UserID");
        userId.setAttribute("AgentDutyCode", "SU");
        userId.setAttribute("POS_Type", "1");
        userId.setAttribute("RequestorType", "U");
        userId.setAttribute("PseudoCityCode", "BOMVS34C3");

    }
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return false;
    }

    @Override
    public void close(MessageContext context) {

    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        WSSecurityHeaderSOAPHandler a = new WSSecurityHeaderSOAPHandler();
//        String nonceValue = RandomStringUtils.random(11, true, true);
        String nonceValue = "secretnonce10111";
        String encodedNonce = Base64.encodeBase64String(nonceValue.getBytes());
        System.out.println(encodedNonce);
        String time = "2015-09-30T14:12:15Z";

        String s1 = encodeToSHA1(nonceValue + time + encodeToSHA1("AMADEUS"));
        String pass = Base64.encodeBase64String(s1.getBytes());
        System.out.println(pass);


        System.out.println(buildPasswordDigest("AMADEUS", nonceValue, time));
    }

}
