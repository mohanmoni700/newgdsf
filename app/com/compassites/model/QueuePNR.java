package com.compassites.model;

import com.compassites.constants.AmadeusConstants;
import play.db.ebean.Model;

import java.util.Date;

/**
 * Created by yaseen on 15-09-2015.
 */
public class QueuePNR extends Model {


    private Long id;

    private String pnr;

    private AmadeusConstants.QUEUE_TYPE queueType;

    private Date creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public AmadeusConstants.QUEUE_TYPE getQueueType() {
        return queueType;
    }

    public void setQueueType(AmadeusConstants.QUEUE_TYPE queueType) {
        this.queueType = queueType;
    }
}
