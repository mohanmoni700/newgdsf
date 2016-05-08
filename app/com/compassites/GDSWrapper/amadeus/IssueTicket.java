/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket.InfantOrAdultAssociation;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket.InfantOrAdultAssociation.PaxDetails;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket.OptionGroup;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket.OptionGroup.Switches;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket.OptionGroup.Switches.StatusDetails;

import java.util.List;

/**
 *
 * @author mahendra-singh
 */
public class IssueTicket {
    public DocIssuanceIssueTicket issue(boolean sendTSTDataForIssuance, List<String> tstReferenceList){
        DocIssuanceIssueTicket tck = new DocIssuanceIssueTicket();
        OptionGroup op = new OptionGroup();
        Switches sw = new Switches();
        StatusDetails sd = new StatusDetails();
        sd.setIndicator("ET");
        sw.setStatusDetails(sd);
        op.setSwitches(sw);
        tck.getOptionGroup().add(op);

        InfantOrAdultAssociation infantOrAdultAssociation = new InfantOrAdultAssociation();
        PaxDetails paxDetails = new PaxDetails();
        paxDetails.setType("A");
        infantOrAdultAssociation.setPaxDetails(paxDetails);
        //commented the association as we may not need it
//        tck.setInfantOrAdultAssociation(infantOrAdultAssociation);


        /*PaxSelection ps=new PaxSelection();
        PassengerReference pr=new PassengerReference();
        pr.setType("PAX");
        pr.setValue("1");
        ps.setPassengerReference(pr);      */



        if(sendTSTDataForIssuance){
            tck.getSelection().add(createTSTReferences(tstReferenceList));
        }
        return tck;
    }

    public DocIssuanceIssueTicket.Selection createTSTReferences(List<String> tstReferenceList){
        DocIssuanceIssueTicket.Selection selection = new DocIssuanceIssueTicket.Selection();
        for(String tstReference : tstReferenceList){
            DocIssuanceIssueTicket.Selection.ReferenceDetails referenceDetails = new DocIssuanceIssueTicket.Selection.ReferenceDetails();
            referenceDetails.setType("TS");
            referenceDetails.setValue(tstReference);
            selection.getReferenceDetails().add(referenceDetails);
        }

        return selection;
    }
    public DocIssuanceIssueTicket issue1(){
        DocIssuanceIssueTicket tck=new DocIssuanceIssueTicket();
        InfantOrAdultAssociation asc=new InfantOrAdultAssociation();
        PaxDetails pd=new PaxDetails();
        pd.setType("A");
        asc.setPaxDetails(pd);
        tck.setInfantOrAdultAssociation(asc);
        return tck;
    }
    
}
