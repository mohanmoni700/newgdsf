package com.compassites.GDSWrapper.amadeus;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.PasswordInfo;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode.DutyCodeDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails.OrganizationDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier.OriginIdentification;
import play.Play;

public class MessageFactory {

	public SecurityAuthenticate buildAuthenticationRequest() {

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
		origin.setSourceOffice(Play.application().configuration().getString("amadeus.SOURCE_OFFICE"));
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