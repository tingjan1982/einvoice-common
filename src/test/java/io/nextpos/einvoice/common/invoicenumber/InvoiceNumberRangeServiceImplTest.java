package io.nextpos.einvoice.common.invoicenumber;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class InvoiceNumberRangeServiceImplTest {

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    InvoiceNumberRangeServiceImplTest(InvoiceNumberRangeService invoiceNumberRangeService) {
        this.invoiceNumberRangeService = invoiceNumberRangeService;
    }

    @Test
    void manageInvoiceNumberRange() throws Exception {

        String ubn = "83515813";
        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, "1090910", "AW", "00002350", "00002359");
        invoiceNumberRange.addNumberRange("GG", "10009000", "10009001");

        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        assertThat(invoiceNumberRange.getId()).isNotNull();
        assertThat(invoiceNumberRange.getNumberRanges()).hasSize(2);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Set<String> newInvoiceNumbers = new HashSet<>();

        final Callable<String> task = () -> {
            try {
                final String invoiceNumber = invoiceNumberRangeService.resolveInvoiceNumber(ubn);
                newInvoiceNumbers.add(invoiceNumber);

                return invoiceNumber;
            } catch (Throwable t) {
                fail("Unexpected exception", t);
                throw t;
            }
        };

        final List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            tasks.add(task);
        }

        executor.invokeAll(tasks);

        assertThat(invoiceNumberRangeService.resolveInvoiceNumber(ubn)).contains("GG");
        assertThat(invoiceNumberRangeService.resolveInvoiceNumber(ubn)).contains("GG");

        final InvoiceNumberRange updatedInvoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRange(invoiceNumberRange.getId());

        assertThat(newInvoiceNumbers).hasSize(10);
        assertThat(newInvoiceNumbers).allSatisfy(n -> assertThat(n.length()).isEqualTo(11));
        assertThat(updatedInvoiceNumberRange.getNumberRanges()).allSatisfy(nr -> assertThat(nr.isFinished()).isTrue());

        assertThatThrownBy(updatedInvoiceNumberRange::findDispensableNumberRange).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> invoiceNumberRangeService.resolveInvoiceNumber(ubn)).isInstanceOf(RuntimeException.class);
    }
}