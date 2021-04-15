package io.nextpos.einvoice.common.invoicenumber;

import io.nextpos.einvoice.common.shared.InvoiceObjectNotFoundException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        final String currentRangeIdentifier = invoiceNumberRangeService.getCurrentRangeIdentifier();
        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, currentRangeIdentifier, "AW", "00002350", "00002359");
        invoiceNumberRange.addNumberRange("GG", "10009000", "10009001");

        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        assertThat(invoiceNumberRange.getId()).isNotNull();
        assertThat(invoiceNumberRange.getStatus()).isEqualByComparingTo(InvoiceNumberRange.InvoiceNumberRangeStatus.ACTIVE);
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

        assertThat(newInvoiceNumbers).hasSize(10);
        assertThat(newInvoiceNumbers).allSatisfy(n -> assertThat(n.length()).isEqualTo(11));

        final InvoiceNumberRange updatedInvoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRange(invoiceNumberRange.getId());

        assertThat(updatedInvoiceNumberRange.getNumberRanges()).allSatisfy(nr -> assertThat(nr.isStarted()).isTrue());
        assertThat(updatedInvoiceNumberRange.getNumberRanges()).allSatisfy(nr -> assertThat(nr.isFinished()).isTrue());

        assertThatThrownBy(updatedInvoiceNumberRange::findDispensableNumberRange).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> invoiceNumberRangeService.resolveInvoiceNumber(ubn)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> invoiceNumberRangeService.resolveInvoiceNumber(ubn)).isInstanceOf(RuntimeException.class);

        assertThat(updatedInvoiceNumberRange.findAvailableNumberRange()).isNotNull();

        updatedInvoiceNumberRange.addNumberRange("HH", "10009050", "10009500");
        invoiceNumberRangeService.saveInvoiceNumberRange(updatedInvoiceNumberRange);

        assertThat(invoiceNumberRangeService.disableOneInvoiceNumberRange(ubn, currentRangeIdentifier, "10009050")
                .findNumberRangeById("10009050")
                .isDisabled()).isTrue();

        assertThatThrownBy(() -> invoiceNumberRangeService.deleteOneInvoiceNumberRange(ubn, currentRangeIdentifier, "10009050")
                .findNumberRangeById("10009050")).isInstanceOf(InvoiceObjectNotFoundException.class);

        invoiceNumberRangeService.deleteInvoiceNumberRange(ubn, invoiceNumberRange.getRangeIdentifier());

        assertThatThrownBy(() -> invoiceNumberRangeService.getInvoiceNumberRange(invoiceNumberRange.getId())).isInstanceOf(InvoiceObjectNotFoundException.class);
    }

    @Test
    void getDateRange() {

        final String rangeIdentifier = invoiceNumberRangeService.getCurrentRangeIdentifier();
        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange("83515813", rangeIdentifier, "AW", "00002350", "00002359");

        final Pair<LocalDateTime, LocalDateTime> dateRange = invoiceNumberRange.getLocalDateTimeRange();
        final LocalDateTime now = LocalDateTime.now();

        assertThat(now.compareTo(dateRange.getLeft())).isGreaterThanOrEqualTo(0);
        assertThat(now.compareTo(dateRange.getRight())).isLessThanOrEqualTo(0);
    }
}