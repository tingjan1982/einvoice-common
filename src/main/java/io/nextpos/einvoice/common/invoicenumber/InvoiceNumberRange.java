package io.nextpos.einvoice.common.invoicenumber;

import io.nextpos.einvoice.common.shared.EInvoiceBaseObject;
import io.nextpos.einvoice.common.shared.InvoiceObjectNotFoundException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.number.NumberStyleFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_ubn_range_identifier_index", def = "{'ubn': 1, 'rangeIdentifier': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvoiceNumberRange extends EInvoiceBaseObject {

    @Id
    private String id;

    private String ubn;

    /**
     * Example: 1090910
     */
    private String rangeIdentifier;

    private InvoiceNumberRangeStatus status;

    private List<NumberRange> numberRanges = new ArrayList<>();

    public InvoiceNumberRange(String ubn, String rangeIdentifier, String prefix, String rangeFrom, String rangeTo) {
        this.ubn = ubn;
        this.rangeIdentifier = rangeIdentifier;
        this.status = InvoiceNumberRangeStatus.ACTIVE;

        this.addNumberRange(prefix, rangeFrom, rangeTo);
    }

    public String getShortRangeIdentifier() {
        return rangeIdentifier.substring(0, 3) + rangeIdentifier.substring(5);
    }

    public void addNumberRange(String prefix, String rangeFrom, String rangeTo) {

        final NumberRange numberRange = new NumberRange(prefix, rangeFrom, rangeTo);
        numberRanges.add(numberRange);
    }

    public NumberRange findDispensableNumberRange() {
        return numberRanges.stream()
                .filter(r -> !r.isFinished() && !r.isDisabled())
                .findFirst().orElseThrow(() -> {
                    throw new RuntimeException("There is no dispensable number range");
                });
    }

    /**
     * Get an unfinished number range or the last number range.
     */
    public NumberRange findAvailableNumberRange() {
        return numberRanges.stream()
                .filter(r -> !r.isFinished())
                .findFirst().orElseGet(() -> numberRanges.get(numberRanges.size() - 1));
    }

    public void disableNumberRangeById(String id) {

        final NumberRange numberRange = this.findNumberRangeById(id);
        numberRange.setDisabled(true);
    }

    public void deleteNumberRangeById(String id) {
        final NumberRange numberRange = this.findNumberRangeById(id);

        if (numberRange.isStarted()) {
            throw new RuntimeException("Started number range cannot be deleted");
        }

        numberRanges.removeIf(nr -> nr.getRangeFrom().equals(id));
    }

    public NumberRange findNumberRangeById(String id) {
        return numberRanges.stream()
                .filter(r -> r.getRangeFrom().equals(id))
                .findFirst().orElseThrow(() -> {
                    throw new InvoiceObjectNotFoundException(NumberRange.class, id);
                });
    }

    @Data
    public static class NumberRange {

        /**
         * Two capitalized letters. Example: AW
         */
        private String prefix;

        private String rangeFrom;

        private String rangeTo;

        private int currentIncrement;

        /**
         * Indicates that the number range has started issuing invoice numbers.
         */
        private boolean started;

        /**
         * Indicates if all numbers have been issued.
         */
        private boolean finished;

        /**
         * Indicates the number range is disabled and cannot be used. Reasons include the number range has been treated as unused numbers
         * and uploaded to big platform.
         */
        private boolean disabled;

        public NumberRange(String prefix, String rangeFrom, String rangeTo) {
            this.prefix = prefix;
            this.rangeFrom = rangeFrom;
            this.rangeTo = rangeTo;
            this.currentIncrement = Integer.parseInt(rangeFrom) - 1;
        }

        public int getRemainingNumberInRange() {
            return Integer.parseInt(rangeTo) - currentIncrement;
        }

        public String getNextIncrement() {
            NumberStyleFormatter formatter = new NumberStyleFormatter("00000000");
            return formatter.getNumberFormat(Locale.getDefault()).format(currentIncrement + 1);
        }

        public boolean isLastNumberInRange() {
            return currentIncrement + 1 == Integer.parseInt(rangeTo);
        }
    }

    public enum InvoiceNumberRangeStatus {

        /**
         * In use
         */
        ACTIVE,

        /**
         * Unused number ranges are uploaded to big platform.
         */
        FINISHED
    }
}
