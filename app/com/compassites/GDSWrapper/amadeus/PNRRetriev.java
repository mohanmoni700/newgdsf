/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnrret_11_3_1a.*;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve.RetrievalFacts;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve.RetrievalFacts.PersonalFacts;
import com.amadeus.xml.tmrcrq_11_1_1a.MiniRuleGetFromPricing;
import com.amadeus.xml.tmrerq_13_1_1a.MiniRuleGetFromETicket;
import com.amadeus.xml.tmrerq_13_1_1a.TicketNumberDetailsTypeI;
import com.amadeus.xml.tmrqrq_11_1_1a.ItemReferencesAndVersionsType;
import com.amadeus.xml.tmrqrq_11_1_1a.MiniRuleGetFromPricingRec;

import java.math.BigInteger;


/**
 *
 * @author mahendra-singh
 */
public class PNRRetriev {
    public PNRRetrieve retrieve(){
        PNRRetrieve rtr=new PNRRetrieve();
        RetrievalFacts rf = new RetrievalFacts();
        RetrievePNRType rv = new RetrievePNRType();
        rv.setType(new BigInteger("3"));

        PersonalFacts pf = new PersonalFacts();
        TravellerInformationType ti = new TravellerInformationType();
        TravellerSurnameInformationTypeI tr=new TravellerSurnameInformationTypeI();
        tr.setSurname("DUPONT");
        ti.setTraveller(tr);
        pf.setTravellerInformation(ti);

        rf.setRetrieve(rv);
        rf.setPersonalFacts(pf);
        rtr.setRetrievalFacts(rf);

        return rtr;
    }
    public PNRRetrieve retrieve(String cnum){
        PNRRetrieve rtr=new PNRRetrieve();
        RetrievalFacts rf=new RetrievalFacts();
        RetrievePNRType rv=new RetrievePNRType();
        rv.setType( new BigInteger("2"));
        rv.setOption1("A");
        rf.setRetrieve(rv);
        ReservationControlInformationType rid = new ReservationControlInformationType();
        ReservationControlInformationDetailsTypeI rs=new ReservationControlInformationDetailsTypeI();
        rs.setControlNumber(cnum);
        rid.setReservation(rs);
        rf.setReservationOrProfileIdentifier(rid);
        rtr.setRetrievalFacts(rf);

        return rtr;
    }

    public PNRRetrieve active(){
        PNRRetrieve rtr=new PNRRetrieve();
        RetrievalFacts rf=new RetrievalFacts();
        RetrievePNRType rv=new RetrievePNRType();
        rv.setType(BigInteger.valueOf(3));
        rv.setOption1("A");
        rf.setRetrieve(rv);
        rtr.setRetrievalFacts(rf);
        return rtr;
    }

    public MiniRuleGetFromPricingRec miniRuleGetFromPricingRec() {
        MiniRuleGetFromPricingRec miniRuleGetFromPricingRec = new MiniRuleGetFromPricingRec();
        //List<ItemReferencesAndVersionsType> itemReferencesAndVersionsTypeList = miniRuleGetFromPricingRec.getRecordId();
        ItemReferencesAndVersionsType itemReferencesAndVersionsType = new ItemReferencesAndVersionsType();
        itemReferencesAndVersionsType.setReferenceType("TST");
        itemReferencesAndVersionsType.setUniqueReference("ALL");
        miniRuleGetFromPricingRec.getRecordId().add(itemReferencesAndVersionsType);
        return miniRuleGetFromPricingRec;
    }

    public MiniRuleGetFromETicket miniRuleGetFromETicket(String ticketNumber) {
        MiniRuleGetFromETicket miniRuleGetFromETicket = new MiniRuleGetFromETicket();
        //List<ItemReferencesAndVersionsType> itemReferencesAndVersionsTypeList = miniRuleGetFromPricingRec.getRecordId();
        TicketNumberDetailsTypeI ticketNumberDetailsTypeI = new TicketNumberDetailsTypeI();
        com.amadeus.xml.tmrerq_13_1_1a.TicketNumberTypeI ticketNumberTypeI = new com.amadeus.xml.tmrerq_13_1_1a.TicketNumberTypeI();
        ticketNumberDetailsTypeI.setNumber(ticketNumber);
        miniRuleGetFromETicket.setTicketNumber(ticketNumberTypeI);
        miniRuleGetFromETicket.getTicketNumber().setDocumentDetails(ticketNumberDetailsTypeI);
        return miniRuleGetFromETicket;
    }

    public MiniRuleGetFromPricing miniRuleGetFromPricing() {
        MiniRuleGetFromPricing miniRuleGetFromPricing = new MiniRuleGetFromPricing();
        com.amadeus.xml.tmrcrq_11_1_1a.ItemReferencesAndVersionsType itemReferencesAndVersionsType = new com.amadeus.xml.tmrcrq_11_1_1a.ItemReferencesAndVersionsType();
        itemReferencesAndVersionsType.setReferenceType("FRN");
        itemReferencesAndVersionsType.setUniqueReference("ALL");
        miniRuleGetFromPricing.getFareRecommendationId().add(itemReferencesAndVersionsType);

        return miniRuleGetFromPricing;
    }
}
