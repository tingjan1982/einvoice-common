package io.nextpos.einvoice.common.invoice;

import java.util.List;

public interface PendingEInvoiceQueueService {

    PendingEInvoiceQueue savePendingEInvoiceQueue(ElectronicInvoice electronicInvoice);

    PendingEInvoiceQueue updatePendingEInvoiceQueue(PendingEInvoiceQueue pendingEInvoiceQueue);

    List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn);

    List<PendingEInvoiceQueue> findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    void deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    List<PendingInvoiceStats> generatePendingEInvoiceStats();
}
