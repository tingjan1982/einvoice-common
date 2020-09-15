package io.nextpos.einvoice.common.invoice;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class PendingEInvoiceQueue extends MongoBaseObject {

    @Id
    private String id;

    private String invoiceNumber;

    private String ubn;

    private PendingEInvoiceStatus status;

    public PendingEInvoiceQueue(String id, String invoiceNumber, String ubn) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.ubn = ubn;
        this.status = PendingEInvoiceStatus.PENDING;
    }

    public void markAsProcessed() {
        status = PendingEInvoiceStatus.PROCESSED;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public enum PendingEInvoiceStatus {

        /**
         * Initial state
         */
        PENDING,

        /**
         * Processed by processor to Turnkey working directory.
         */
        PROCESSED
    }
}
