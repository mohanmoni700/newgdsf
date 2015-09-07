/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnrret_11_3_1a.*;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve.RetrievalFacts;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve.RetrievalFacts.PersonalFacts;

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
}
