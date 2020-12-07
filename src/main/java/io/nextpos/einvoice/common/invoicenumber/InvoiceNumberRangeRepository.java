package io.nextpos.einvoice.common.invoicenumber;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceNumberRangeRepository extends MongoRepository<InvoiceNumberRange, String> {

    Optional<InvoiceNumberRange> findByUbnAndRangeIdentifier(String ubn, String rangeIdentifier);

    List<InvoiceNumberRange> findAllByUbnOrderByRangeIdentifier(String ubn);

    List<InvoiceNumberRange> findAllByRangeIdentifierInOrderByRangeIdentifier(List<String> rangeIdentifiers);

    List<InvoiceNumberRange> findAllByRangeIdentifierAndStatus(String rangeIdentifier, InvoiceNumberRange.InvoiceNumberRangeStatus status);
}
