package services;

import com.compassites.model.CancelPNRResponse;

/**
 * Created by Yaseen on 08-05-2015.
 */
public interface CancelService {

    public CancelPNRResponse cancelPNR(String pnr,Boolean isFullPNR);

}
