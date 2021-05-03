package io.nextpos.einvoice.common.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PendingEInvoiceQueueRepository extends MongoRepository<PendingEInvoiceQueue, String> {

    PendingEInvoiceQueue findByInvoiceNumberAndInvoiceTypeAndStatus(String invoiceNumber, PendingEInvoiceQueue.PendingEInvoiceType invoiceType, PendingEInvoiceQueue.PendingEInvoiceStatus status);

    List<PendingEInvoiceQueue> findAllByUbnAndStatus(String ubn, PendingEInvoiceQueue.PendingEInvoiceStatus status);

    List<PendingEInvoiceQueue> findAllByStatusIn(List<PendingEInvoiceQueue.PendingEInvoiceStatus> status);

    void deleteAllByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);
}
