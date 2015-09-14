package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.qdqlrq_11_1_1a.*;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

/**
 * Created by ritesh on 9/10/15.
 */
public class QueueListReq {

    private final static String SOURCE_QUALIFIER = "4";
    private final static String IDENTIFICATION_TYPE = "C";
    private final static String ITEM_NUMBER = "1";
    private final static String SOURCE_OFFICE = "SOURCE_OFFICE";
    private final static Long QUEUE_NUM_ONE = 1L;
    private final static Long QUEUE_NUM_SEVEN = 7L;
    private final static Long QUEUE_NUM_TWELVE = 12L;

    private static Properties getPropertyFileRef(){
        Properties prop = new Properties();
        String propFileName = "conf/amadeus_config.properties";
        try {
            InputStream inputStream = new FileInputStream(propFileName);
            if (inputStream != null) {
                try {
                    prop.load(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return prop;
    }

    private static QueueList buildQueueReq(){

        Properties prop = getPropertyFileRef();
        QueueList queueList = new QueueList();
         /*
            Target Office Details
         */
        AdditionalBusinessSourceInformationTypeI additionalBusinessSourceInformationTypeI = new AdditionalBusinessSourceInformationTypeI();
        SourceTypeDetailsTypeI sourceTypeDetailsTypeI = new SourceTypeDetailsTypeI();
        sourceTypeDetailsTypeI.setSourceQualifier1(SOURCE_QUALIFIER);
        OriginatorIdentificationDetailsTypeI originatorIdentificationDetailsTypeI = new OriginatorIdentificationDetailsTypeI();
        originatorIdentificationDetailsTypeI.setInHouseIdentification1(prop.getProperty(SOURCE_OFFICE));
        additionalBusinessSourceInformationTypeI.setSourceType(sourceTypeDetailsTypeI);
        additionalBusinessSourceInformationTypeI.setOriginatorDetails(originatorIdentificationDetailsTypeI);
        queueList.setTargetOffice(additionalBusinessSourceInformationTypeI);

        /*
            Category Details
         */
//        SubQueueInformationTypeI subQueueInformationTypeI = new SubQueueInformationTypeI();
//        SubQueueInformationDetailsTypeI subQueueInformationDetailsTypeI = new SubQueueInformationDetailsTypeI();
//        subQueueInformationDetailsTypeI.setIdentificationType(IDENTIFICATION_TYPE);
//        subQueueInformationDetailsTypeI.setItemNumber(ITEM_NUMBER);
//        subQueueInformationTypeI.setSubQueueInfoDetails(subQueueInformationDetailsTypeI);
//        queueList.setCategoryDetails(subQueueInformationTypeI);

        return queueList;

    }

    public static QueueList getConfirmQueueRequest(){
        QueueList queueList = buildQueueReq();
        QueueInformationTypeI queueInformationTypeI = new QueueInformationTypeI();
        QueueInformationDetailsTypeI queueInformationDetailsTypeI = new QueueInformationDetailsTypeI();
        queueInformationDetailsTypeI.setNumber(BigInteger.valueOf(QUEUE_NUM_ONE));
        queueInformationTypeI.setQueueDetails(queueInformationDetailsTypeI);
        queueList.setQueueNumber(queueInformationTypeI);
        return queueList;
    }

    public static QueueList getScheduleChangesQueueRequest(){
        QueueList queueList = buildQueueReq();
        QueueInformationTypeI queueInformationTypeI = new QueueInformationTypeI();
        QueueInformationDetailsTypeI queueInformationDetailsTypeI = new QueueInformationDetailsTypeI();
        queueInformationDetailsTypeI.setNumber(BigInteger.valueOf(QUEUE_NUM_SEVEN));
        queueInformationTypeI.setQueueDetails(queueInformationDetailsTypeI);
        queueList.setQueueNumber(queueInformationTypeI);
        return queueList;
    }

    public static QueueList getExpiryTimeQueueRequest(){
        QueueList queueList = buildQueueReq();
        QueueInformationTypeI queueInformationTypeI = new QueueInformationTypeI();
        QueueInformationDetailsTypeI queueInformationDetailsTypeI = new QueueInformationDetailsTypeI();
        queueInformationDetailsTypeI.setNumber(BigInteger.valueOf(QUEUE_NUM_TWELVE));
        queueInformationTypeI.setQueueDetails(queueInformationDetailsTypeI);
        queueList.setQueueNumber(queueInformationTypeI);
        return queueList;
    }
}
