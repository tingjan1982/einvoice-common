package io.nextpos.einvoice.common.invoicenumber;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InvoiceNumberRangeRepository extends MongoRepository<InvoiceNumberRange, String> {

    InvoiceNumberRange findByUbnAndRangeIdentifier(String ubn, String rangeIdentifier);

    List<InvoiceNumberRange> findAllByUbnOrderByRangeIdentifier(String ubn);
}
