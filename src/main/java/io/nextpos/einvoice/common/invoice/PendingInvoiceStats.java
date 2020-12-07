package io.nextpos.einvoice.common.invoice;

import lombok.Data;

@Data
public class PendingInvoiceStats {

    private String ubn;

    private String status;

    private int invoiceCount;
}
