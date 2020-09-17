package io.nextpos.einvoice.common.invoicenumber;

import java.util.List;

public interface InvoiceNumberRangeService {

    InvoiceNumberRange saveInvoiceNumberRange(InvoiceNumberRange invoiceNumberRange);

    InvoiceNumberRange getInvoiceNumberRange(String id);

    InvoiceNumberRange getCurrentInvoiceNumberRange(String ubn);

    InvoiceNumberRange getInvoiceNumberRangeByRangeIdentifier(String ubn, String rangeIdentifier);

    List<InvoiceNumberRange> getInvoiceNumberRanges(String ubn);

    String resolveInvoiceNumber(String ubn);

    String getCurrentRangeIdentifier();
}
