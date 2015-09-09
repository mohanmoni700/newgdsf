package services;

import com.amadeus.xml.qdqlrq_11_1_1a.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

/**
 * Created by ritesh on 9/7/15.
 */
public class AmadeusQueueListInfoService {

    private static Properties readQueueListPropertyFile(){
        Properties prop = new Properties();
        String propFileName = "conf/amadeusQueueListDetails.properties";
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

    public static QueueList getQueueListRequest(){

        Properties prop = readQueueListPropertyFile();

        QueueList queueList = new QueueList();
        /*
            Target Office Details
         */
        AdditionalBusinessSourceInformationTypeI additionalBusinessSourceInformationTypeI = new AdditionalBusinessSourceInformationTypeI();
        SourceTypeDetailsTypeI sourceTypeDetailsTypeI = new SourceTypeDetailsTypeI();
        sourceTypeDetailsTypeI.setSourceQualifier1("4");
        OriginatorIdentificationDetailsTypeI originatorIdentificationDetailsTypeI = new OriginatorIdentificationDetailsTypeI();
        originatorIdentificationDetailsTypeI.setInHouseIdentification1("BOMVS34C3");
        additionalBusinessSourceInformationTypeI.setSourceType(sourceTypeDetailsTypeI);
        additionalBusinessSourceInformationTypeI.setOriginatorDetails(originatorIdentificationDetailsTypeI);
        queueList.setTargetOffice(additionalBusinessSourceInformationTypeI);

        /*
            Queue Number Details
         */
        QueueInformationTypeI queueInformationTypeI = new QueueInformationTypeI();
        QueueInformationDetailsTypeI queueInformationDetailsTypeI = new QueueInformationDetailsTypeI();
        queueInformationDetailsTypeI.setNumber(BigInteger.valueOf(7L));
        queueInformationTypeI.setQueueDetails(queueInformationDetailsTypeI);
        queueList.setQueueNumber(queueInformationTypeI);

        /*
            Category Details
         */
        SubQueueInformationTypeI subQueueInformationTypeI = new SubQueueInformationTypeI();
        SubQueueInformationDetailsTypeI subQueueInformationDetailsTypeI = new SubQueueInformationDetailsTypeI();
        subQueueInformationDetailsTypeI.setIdentificationType("C");
        subQueueInformationDetailsTypeI.setItemNumber("0");
        subQueueInformationTypeI.setSubQueueInfoDetails(subQueueInformationDetailsTypeI);
        queueList.setCategoryDetails(subQueueInformationTypeI);

        return queueList;

    }
}
