package io.nextpos.einvoice.common.invoice;

import java.util.List;
import java.util.Map;

public interface PendingEInvoiceQueueService {

    PendingEInvoiceQueue createPendingEInvoiceQueue(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType pendingEInvoiceType);

    PendingEInvoiceQueue updatePendingEInvoiceQueue(PendingEInvoiceQueue pendingEInvoiceQueue);

    List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn);

    List<PendingEInvoiceQueue> findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus... status);

    void deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    Map<String, List<PendingInvoiceStats>> generatePendingEInvoiceStats();
}
