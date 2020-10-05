package io.nextpos.einvoice.common.invoice;

import java.util.List;

public interface PendingEInvoiceQueueService {

    PendingEInvoiceQueue createPendingEInvoiceQueue(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType pendingEInvoiceType);

    PendingEInvoiceQueue updatePendingEInvoiceQueue(PendingEInvoiceQueue pendingEInvoiceQueue);

    List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn);

    List<PendingEInvoiceQueue> findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    void deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    List<PendingInvoiceStats> generatePendingEInvoiceStats();
}
