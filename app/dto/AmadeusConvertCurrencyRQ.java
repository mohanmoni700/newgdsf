package dto;

import java.math.BigDecimal;

public class AmadeusConvertCurrencyRQ {

    private String fromCurrency;

    private String toCurrency;

    private String fromLocation;

    private String toLocation;

    private BigDecimal amountToConvert;

    private BigDecimal convertForRate;

    //DDMMYY format
    private String convertForDate;

    private boolean isConvertGivenAmount;

    private boolean isConvertForGivenRate;

    private boolean isConvertBasedOnLocation;

    private boolean isConvertBasedOnDate;

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getAmountToConvert() {
        return amountToConvert;
    }

    public void setAmountToConvert(BigDecimal amountToConvert) {
        this.amountToConvert = amountToConvert;
    }

    public boolean isConvertGivenAmount() {
        return isConvertGivenAmount;
    }

    public void setConvertGivenAmount(boolean convertGivenAmount) {
        isConvertGivenAmount = convertGivenAmount;
    }

    public boolean isConvertForGivenRate() {
        return isConvertForGivenRate;
    }

    public void setConvertForGivenRate(boolean convertForGivenRate) {
        isConvertForGivenRate = convertForGivenRate;
    }

    public String getConvertForDate() {
        return convertForDate;
    }

    public void setConvertForDate(String convertForDate) {
        this.convertForDate = convertForDate;
    }

    public BigDecimal getConvertForRate() {
        return convertForRate;
    }

    public void setConvertForRate(BigDecimal convertForRate) {
        this.convertForRate = convertForRate;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public boolean isConvertBasedOnLocation() {
        return isConvertBasedOnLocation;
    }

    public void setConvertBasedOnLocation(boolean convertBasedOnLocation) {
        isConvertBasedOnLocation = convertBasedOnLocation;
    }

    public boolean isConvertBasedOnDate() {
        return isConvertBasedOnDate;
    }

    public void setConvertBasedOnDate(boolean convertBasedOnDate) {
        isConvertBasedOnDate = convertBasedOnDate;
    }

}
