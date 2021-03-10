package io.nextpos.einvoice.common.shared;

public class InvoiceObjectNotFoundException extends RuntimeException {

    public InvoiceObjectNotFoundException(String message) {
        super(message);
    }

    public InvoiceObjectNotFoundException(Class<?> dataObject, String id) {
        super(String.format("Invoice Object [%s] not found: %s", dataObject.getSimpleName(), id));
    }
}
