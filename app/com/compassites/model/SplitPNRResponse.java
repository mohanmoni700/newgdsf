package com.compassites.model;

/**
 * Created by sathishkumarpalanisamy on 27/09/17.
 */
public class SplitPNRResponse {

    private PNRResponse pnrResponse;


    private CancelPNRResponse cancelPNRResponse;

    public PNRResponse getPnrResponse() {
        return pnrResponse;
    }

    public void setPnrResponse(PNRResponse pnrResponse) {
        this.pnrResponse = pnrResponse;
    }

    public CancelPNRResponse getCancelPNRResponse() {
        return cancelPNRResponse;
    }

    public void setCancelPNRResponse(CancelPNRResponse cancelPNRResponse) {
        this.cancelPNRResponse = cancelPNRResponse;
    }

}
