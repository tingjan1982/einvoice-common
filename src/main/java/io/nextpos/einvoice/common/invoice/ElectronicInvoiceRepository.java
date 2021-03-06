package io.nextpos.einvoice.common.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ElectronicInvoiceRepository extends MongoRepository<ElectronicInvoice, String> {

    Optional<ElectronicInvoice> findByInternalInvoiceNumber(String internalInvoiceNumber);

    List<ElectronicInvoice> findAllByClientIdAndInvoiceStatus(String clientId, ElectronicInvoice.InvoiceStatus invoiceStatus);
}
