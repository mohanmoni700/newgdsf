package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode.DutyCodeDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.PasswordInfo;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails.OrganizationDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier.OriginIdentification;
import play.Play;

import java.math.BigDecimal;
import java.util.HashMap;

public class MessageFactory {  //todo rename

	private static MessageFactory instance;
	private HashMap<String, SecurityAuthenticate> securityAuthMap;

	private MessageFactory() {
		// private constructor
		this.securityAuthMap = new HashMap<>();
	}

	public static MessageFactory getInstance() {
		if (instance == null) {
			instance = new MessageFactory();
		}
		return instance;
	}

	//todo
	public SecurityAuthenticate getAuthenticationRequest(){
		//String officeId = Play.application().configuration().getString("amadeus.SOURCE_OFFICE_DEFAULT");
		return getAuthenticationRequest("BOMVS34C3");
	}
	public SecurityAuthenticate getAuthenticationRequest(String officeId) {
		SecurityAuthenticate authenticateRequest = securityAuthMap.get(officeId);
		if(authenticateRequest == null){
			authenticateRequest = buildAuthenticationRequest(officeId);
			securityAuthMap.put(officeId, authenticateRequest);
		}
		return authenticateRequest;
	}

	public SecurityAuthenticate buildAuthenticationRequest(String officeId) {

       		// object factory
		com.amadeus.xml.vlsslq_06_1_1a.ObjectFactory factory = new com.amadeus.xml.vlsslq_06_1_1a.ObjectFactory();

		// authenticate request
		SecurityAuthenticate authenticateRequest = factory
				.createSecurityAuthenticate();

		// userIdentifier
		UserIdentifier userId = factory
				.createSecurityAuthenticateUserIdentifier();
		userId.setOriginator(Play.application().configuration().getString("amadeus.ORIGINATOR"));
		userId.setOriginatorTypeCode(Play.application().configuration().getString("amadeus.ORIGINATOR_TYPE_CODE"));
		OriginIdentification origin = factory
				.createSecurityAuthenticateUserIdentifierOriginIdentification();
		//origin.setSourceOffice(Play.application().configuration().getString("amadeus.SOURCE_OFFICE"));
		origin.setSourceOffice(officeId);
		userId.setOriginIdentification(origin);
		authenticateRequest.getUserIdentifier().add(userId);

		// dutyCode
		DutyCode dutycode = factory.createSecurityAuthenticateDutyCode();
		DutyCodeDetails dutyCodeDetails = factory
				.createSecurityAuthenticateDutyCodeDutyCodeDetails();
		dutyCodeDetails.setReferenceIdentifier(Play.application().configuration().getString("amadeus.REFERENCE_IDENTIFIER"));
		dutyCodeDetails.setReferenceQualifier(Play.application().configuration().getString("amadeus.REFERENCE_QUALIFIER"));
		dutycode.setDutyCodeDetails(dutyCodeDetails);
		authenticateRequest.setDutyCode(dutycode);

		// systemDetails
		SystemDetails systemDetails = factory
				.createSecurityAuthenticateSystemDetails();
		OrganizationDetails organizationDetails = factory
				.createSecurityAuthenticateSystemDetailsOrganizationDetails();
		organizationDetails.setOrganizationId(Play.application().configuration().getString("amadeus.ORGANISATION_ID"));
		systemDetails.setOrganizationDetails(organizationDetails);
		authenticateRequest.setSystemDetails(systemDetails);

		// passwordInfo
		PasswordInfo passwordInfo = factory
				.createSecurityAuthenticatePasswordInfo();
		passwordInfo.setDataType(Play.application().configuration().getString("amadeus.DATA_TYPE"));
		String binary = Play.application().configuration().getString("amadeus.BINARY_DATA");
		passwordInfo.setBinaryData(binary);
		passwordInfo.setDataLength(new BigDecimal(Play.application().configuration().getString("amadeus.DATA_LENGTH")));

		authenticateRequest.getPasswordInfo().add(passwordInfo);
		return authenticateRequest;
	}


}