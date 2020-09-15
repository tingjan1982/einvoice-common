package io.nextpos.einvoice.common.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PendingEInvoiceQueueRepository extends MongoRepository<PendingEInvoiceQueue, String> {

    List<PendingEInvoiceQueue> findAllByUbnAndStatus(String ubn, PendingEInvoiceQueue.PendingEInvoiceStatus status);

    List<PendingEInvoiceQueue> findAllByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);

    void deleteAllByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status);
}
