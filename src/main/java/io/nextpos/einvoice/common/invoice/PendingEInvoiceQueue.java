package io.nextpos.einvoice.common.invoice;

import io.nextpos.einvoice.common.shared.EInvoiceBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PendingEInvoiceQueue extends EInvoiceBaseObject {

    @Id
    private String id;

    private String invoiceNumber;

    private String ubn;

    private PendingEInvoiceType invoiceType;

    @Indexed
    private PendingEInvoiceStatus status;

    @DBRef
    private ElectronicInvoice electronicInvoice;

    public PendingEInvoiceQueue(ElectronicInvoice electronicInvoice, PendingEInvoiceType pendingEInvoiceType) {
        this.invoiceNumber = electronicInvoice.getInvoiceNumber();
        this.ubn = electronicInvoice.getSellerUbn();
        this.electronicInvoice = electronicInvoice;

        this.invoiceType = pendingEInvoiceType;
        this.status = PendingEInvoiceStatus.PENDING;
    }

    public void markAsProcessed() {
        status = PendingEInvoiceStatus.PROCESSED;
    }

    public enum PendingEInvoiceType {

        /**
         * New invoice (C0401)
         */
        CREATE,

        /**
         * Cancel invoice (C0501)
         */
        CANCEL,

        /**
         * Void invoice (C0701)
         */
        VOID
    }

    public enum PendingEInvoiceStatus {

        /**
         * Initial state - ready to be processed.
         */
        PENDING,

        /**
         * MIG is copied to Turnkey working directory.
         */
        PROCESSED,

        /**
         * Uploaded to big platform.
         */
        UPLOADED,

        /**
         * Confirmed by big platform.
         */
        CONFIRMED,

        /**
         * Error returned from big platform.
         */
        ERROR
    }
}
