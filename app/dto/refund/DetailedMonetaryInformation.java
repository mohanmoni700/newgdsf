package dto.refund;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DetailedMonetaryInformation {

    private String qualifier;

    private BigDecimal amount;

    private String currency;

    private BigInteger decimalPlaces;

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigInteger getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(BigInteger decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }


}
