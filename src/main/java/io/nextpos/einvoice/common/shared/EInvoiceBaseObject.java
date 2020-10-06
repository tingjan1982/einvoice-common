package io.nextpos.einvoice.common.shared;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Data
public abstract class EInvoiceBaseObject implements Persistable<String> {

    /**
     * https://stackoverflow.com/questions/25333711/what-is-the-use-of-the-temporal-annotation-in-hibernate
     */
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    private Date modifiedDate;

    /**
     * This is used to search turnkey database to find e-invoice processing result.
     */
    private String invoiceIdentifier;

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}
