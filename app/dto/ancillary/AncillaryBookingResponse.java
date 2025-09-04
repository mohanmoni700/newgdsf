package dto.ancillary;

public class AncillaryBookingResponse {

    private String code;
    private boolean chargeable;
    private String segmentRefId;
    private String amadeusPaxRef;
    private String currency;
    private String amount;
    private String errorMessage;
    private String errorCode;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAmadeusPaxRef() {
        return amadeusPaxRef;
    }

    public void setAmadeusPaxRef(String amadeusPaxRef) {
        this.amadeusPaxRef = amadeusPaxRef;
    }

    public boolean isChargeable() {
        return chargeable;
    }

    public void setChargeable(boolean chargeable) {
        this.chargeable = chargeable;
    }

    public String getSegmentRefId() {
        return segmentRefId;
    }

    public void setSegmentRefId(String segmentRefId) {
        this.segmentRefId = segmentRefId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
