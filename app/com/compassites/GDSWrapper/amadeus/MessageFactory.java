package com.compassites.GDSWrapper.amadeus;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.PasswordInfo;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.DutyCode.DutyCodeDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.SystemDetails.OrganizationDetails;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate.UserIdentifier.OriginIdentification;

public class MessageFactory {

	public SecurityAuthenticate buildAuthenticationRequest() {

        Properties prop = new Properties();
        try {
            //load a properties file from class path, inside static method
            prop.load(getClass().getClassLoader().getResourceAsStream("amadeus_config.properties"));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

		// object factory
		com.amadeus.xml.vlsslq_06_1_1a.ObjectFactory factory = new com.amadeus.xml.vlsslq_06_1_1a.ObjectFactory();

		// authenticate request
		SecurityAuthenticate authenticateRequest = factory
				.createSecurityAuthenticate();

		// userIdentifier
		UserIdentifier userId = factory
				.createSecurityAuthenticateUserIdentifier();
		userId.setOriginator(prop.getProperty("ORIGINATOR"));
		userId.setOriginatorTypeCode(prop.getProperty("ORIGINATOR_TYPE_CODE"));
		OriginIdentification origin = factory
				.createSecurityAuthenticateUserIdentifierOriginIdentification();
		origin.setSourceOffice(prop.getProperty("SOURCE_OFFICE"));
		userId.setOriginIdentification(origin);
		authenticateRequest.getUserIdentifier().add(userId);

		// dutyCode
		DutyCode dutycode = factory.createSecurityAuthenticateDutyCode();
		DutyCodeDetails dutyCodeDetails = factory
				.createSecurityAuthenticateDutyCodeDutyCodeDetails();
		dutyCodeDetails.setReferenceIdentifier(prop.getProperty("REFERENCE_IDENTIFIER"));
		dutyCodeDetails.setReferenceQualifier(prop.getProperty("REFERENCE_QUALIFIER"));
		dutycode.setDutyCodeDetails(dutyCodeDetails);
		authenticateRequest.setDutyCode(dutycode);

		// systemDetails
		SystemDetails systemDetails = factory
				.createSecurityAuthenticateSystemDetails();
		OrganizationDetails organizationDetails = factory
				.createSecurityAuthenticateSystemDetailsOrganizationDetails();
		organizationDetails.setOrganizationId(prop.getProperty("ORGANISATION_ID"));
		systemDetails.setOrganizationDetails(organizationDetails);
		authenticateRequest.setSystemDetails(systemDetails);

		// passwordInfo
		PasswordInfo passwordInfo = factory
				.createSecurityAuthenticatePasswordInfo();
		passwordInfo.setDataLength(BigDecimal.valueOf(7));
		passwordInfo.setDataType(prop.getProperty("DATA_TYPE"));
		passwordInfo.setBinaryData(prop.getProperty("BINARY_DATA"));
		authenticateRequest.getPasswordInfo().add(passwordInfo);

		return authenticateRequest;
	}


}