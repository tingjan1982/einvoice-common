package io.nextpos.einvoice.common.shared;

public class InvoiceObjectNotFoundException extends RuntimeException {

    public InvoiceObjectNotFoundException(String message) {
        super(message);
    }
}
