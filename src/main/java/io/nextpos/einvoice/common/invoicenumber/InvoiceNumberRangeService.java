package io.nextpos.einvoice.common.invoicenumber;

import java.time.YearMonth;
import java.util.List;

public interface InvoiceNumberRangeService {

    InvoiceNumberRange saveInvoiceNumberRange(InvoiceNumberRange invoiceNumberRange);

    InvoiceNumberRange getInvoiceNumberRange(String id);

    boolean hasCurrentInvoiceNumberRange(String ubn);

    InvoiceNumberRange getCurrentInvoiceNumberRange(String ubn);

    InvoiceNumberRange getInvoiceNumberRangeByRangeIdentifier(String ubn, String rangeIdentifier);

    List<InvoiceNumberRange> getInvoiceNumberRanges(String ubn);

    List<InvoiceNumberRange> getInvoiceNumberRangesByLastRangeIdentifier();

    void deleteOneInvoiceNumberRange(String ubn, String rangeIdentifier, String rangeFrom);

    void deleteInvoiceNumberRange(String ubn, String rangeIdentifier);

    String resolveInvoiceNumber(String ubn);

    String getCurrentRangeIdentifier();

    String getRangeIdentifier(YearMonth yearMonth);
}
