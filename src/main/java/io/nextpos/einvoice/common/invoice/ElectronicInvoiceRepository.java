package io.nextpos.einvoice.common.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ElectronicInvoiceRepository extends MongoRepository<ElectronicInvoice, String> {

}
