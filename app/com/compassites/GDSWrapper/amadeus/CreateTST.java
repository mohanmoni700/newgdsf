/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compassites.GDSWrapper.amadeus;


import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing.PsaList;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing.PsaList.ItemReference;
import com.amadeus.xml.ttstrq_13_1_1a.CodedAttributeInformationType;
import com.amadeus.xml.ttstrq_13_1_1a.CodedAttributeType;
import com.amadeus.xml.ttstrq_13_1_1a.TicketDisplayTST;

import java.math.BigDecimal;

/**
 *
 * @author mahendra-singh
 */
public class CreateTST {

    public TicketCreateTSTFromPricing createTSTReq(int numberOfTST) {
        TicketCreateTSTFromPricing tst = new TicketCreateTSTFromPricing();
        for(int i = 1; i <= numberOfTST ; i++){
            tst.getPsaList().add(getpsa(""+i));
        }

//        tst.getPsaList().add(getpsa("2"));
        //tst.getPsaList().add(getpsa("3"));
        //tst.getPsaList().add(getpsa("4"));

        return tst;
    }

    public PsaList getpsa(String ref) {
        PsaList psa = new PsaList();
        ItemReference ir = new ItemReference();
        ir.setReferenceType("TST");
        ir.setUniqueReference(new BigDecimal(ref));
        psa.setItemReference(ir);
        return psa;
    }

    public TicketDisplayTST createTicketDisplayTSTReq(){
        TicketDisplayTST ticketDisplayTST = new TicketDisplayTST();
        CodedAttributeType codedAttributeType = new CodedAttributeType();
        CodedAttributeInformationType codedAttributeInformationType = new CodedAttributeInformationType();
        codedAttributeInformationType.setAttributeType("ALL");
        codedAttributeType.setAttributeDetails(codedAttributeInformationType);
        ticketDisplayTST.setDisplayMode(codedAttributeType);

        return ticketDisplayTST;
    }
}
