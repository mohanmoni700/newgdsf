/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.PersonalFacts;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.PersonalFacts.TravellerInformation;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.PersonalFacts.TravellerInformation.Traveller;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.ReservationOrProfileIdentifier;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.ReservationOrProfileIdentifier.Reservation;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve.RetrievalFacts.Retrieve;

/**
 *
 * @author mahendra-singh
 */
public class PNRRetriev {
    public PNRRetrieve retrieve(){
        PNRRetrieve rtr=new PNRRetrieve();
        RetrievalFacts rf=new RetrievalFacts();
        Retrieve rv=new Retrieve();
        rv.setType("3");
        
        PersonalFacts pf=new PersonalFacts();
        TravellerInformation ti=new TravellerInformation();
        Traveller tr=new Traveller();
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
        Retrieve rv=new Retrieve();
        rv.setType("7");
        rf.setRetrieve(rv);
        ReservationOrProfileIdentifier rid=new ReservationOrProfileIdentifier();
        Reservation rs=new Reservation();
        rs.setControlNumber(cnum);
        rid.setReservation(rs);
        rf.setReservationOrProfileIdentifier(rid);
        rtr.setRetrievalFacts(rf);
        
        return rtr;
    }
    
    public PNRRetrieve active(){
        PNRRetrieve rtr=new PNRRetrieve();
        RetrievalFacts rf=new RetrievalFacts();
        Retrieve rv=new Retrieve();
        rv.setType("3");
        rv.setOption1("A");
        rf.setRetrieve(rv);       
        rtr.setRetrievalFacts(rf);        
        return rtr;
    }
}
