package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.fcuqcq_08_1_1a.FareConvertCurrency;
import com.amadeus.xml.fcuqcq_08_1_1a.FareConvertCurrency.*;
import dto.AmadeusConvertCurrencyRQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FareConvertCurrencyReqBuilder {

    static Logger logger = LoggerFactory.getLogger("gds");

    //Have not built request for Location wise conversion, user specified rate and date (Will take up based on requirements)
    public static FareConvertCurrency buildFareConvertCurrencyQuery(AmadeusConvertCurrencyRQ amadeusConvertCurrencyRQ) {

        FareConvertCurrency fareConvertCurrency = new FareConvertCurrency();

        try {
            //Strictly 726 for all conversion operations
            Message message = new FareConvertCurrency.Message();
            Message.MessageFunctionDetails messageFunctionDetails = new Message.MessageFunctionDetails();
            String messageFunction = "726";
            messageFunctionDetails.setMessageFunction(messageFunction);
            message.setMessageFunctionDetails(messageFunctionDetails);

            fareConvertCurrency.setMessage(message);

            List<ConversionDetails> currencyConversionDetailsList = new ArrayList<>();

            //Setting fromCurrency Details here
            //Setting identifiers for fromCurrency here (fromCurrency = 706)
            ConversionDetails fromCurrencyConversionDetails = new ConversionDetails();
            ConversionDetails.ConversionDirection fromCurrencyConversionDirection = new ConversionDetails.ConversionDirection();
            ConversionDetails.ConversionDirection.SelectionDetails fromCurrencySelectionDetails = new ConversionDetails.ConversionDirection.SelectionDetails();
            fromCurrencySelectionDetails.setOption("706");
            fromCurrencyConversionDirection.getSelectionDetails().add(fromCurrencySelectionDetails);

            fromCurrencyConversionDetails.setConversionDirection(fromCurrencyConversionDirection);

            //Setting From Currency here
            ConversionDetails.CurrencyInfo fromCurrencyInfo = new ConversionDetails.CurrencyInfo();
            ConversionDetails.CurrencyInfo.ConversionRateDetails fromCurrencyConversionRateDetails = new ConversionDetails.CurrencyInfo.ConversionRateDetails();
            String fromCurrency = amadeusConvertCurrencyRQ.getFromCurrency();
            fromCurrencyConversionRateDetails.setCurrency(fromCurrency);
            fromCurrencyInfo.setConversionRateDetails(fromCurrencyConversionRateDetails);

            fromCurrencyConversionDetails.setCurrencyInfo(fromCurrencyInfo);

            //If an amount is given then that amount gets converted
            if (amadeusConvertCurrencyRQ.isConvertGivenAmount()) {
                ConversionDetails.AmountInfo amountInfo = new ConversionDetails.AmountInfo();

                ConversionDetails.AmountInfo.MonetaryDetails monetaryDetails = new ConversionDetails.AmountInfo.MonetaryDetails();
                monetaryDetails.setTypeQualifier("B");
                monetaryDetails.setAmount(amadeusConvertCurrencyRQ.getAmountToConvert().toString());
                amountInfo.getMonetaryDetails().add(monetaryDetails);

                fromCurrencyConversionDetails.setAmountInfo(amountInfo);
            }
            currencyConversionDetailsList.add(fromCurrencyConversionDetails);


            //Setting toCurrency Details here
            //Setting identifiers for toCurrency here (toCurrency = 707)
            ConversionDetails toCurrencyConversionDetails = new ConversionDetails();
            ConversionDetails.ConversionDirection toCurrencyConversionDirection = new ConversionDetails.ConversionDirection();
            ConversionDetails.ConversionDirection.SelectionDetails toCurrencySelectionDetails = new ConversionDetails.ConversionDirection.SelectionDetails();
            toCurrencySelectionDetails.setOption("707");
            toCurrencyConversionDirection.getSelectionDetails().add(toCurrencySelectionDetails);

            toCurrencyConversionDetails.setConversionDirection(toCurrencyConversionDirection);

            //Setting to Currency here
            ConversionDetails.CurrencyInfo toCurrencyInfo = new ConversionDetails.CurrencyInfo();
            ConversionDetails.CurrencyInfo.ConversionRateDetails toCurrencyConversionRateDetails = new ConversionDetails.CurrencyInfo.ConversionRateDetails();
            String toCurrency = amadeusConvertCurrencyRQ.getToCurrency();
            toCurrencyConversionRateDetails.setCurrency(toCurrency);
            toCurrencyInfo.setConversionRateDetails(toCurrencyConversionRateDetails);

            toCurrencyConversionDetails.setCurrencyInfo(toCurrencyInfo);

            currencyConversionDetailsList.add(toCurrencyConversionDetails);

            fareConvertCurrency.getConversionDetails().addAll(currencyConversionDetailsList);

            return fareConvertCurrency;
        } catch (Exception e) {
            logger.debug("Error while building FareConvertCurrency request body {} ", e.getMessage(), e);
            return null;
        }
    }

}
