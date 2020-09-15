package io.nextpos.einvoice.common.invoice;

import java.util.List;

public interface PendingEInvoiceQueueService {

    PendingEInvoiceQueue savePendingEInvoiceQueue(ElectronicInvoice electronicInvoice);

    List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn);

    List<PendingEInvoiceQueue> findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    PendingInvoiceStats generatePendingEInvoiceStats();
}
