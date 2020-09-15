package io.nextpos.einvoice.common.invoice;

import lombok.Data;

@Data
public class PendingInvoiceStats {

    private String id;

    private int invoiceCount;
}
