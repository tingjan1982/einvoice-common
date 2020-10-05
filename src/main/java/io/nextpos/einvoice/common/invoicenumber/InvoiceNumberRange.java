package io.nextpos.einvoice.common.invoicenumber;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_ubn_range_identifier_index", def = "{'ubn': 1, 'rangeIdentifier': 1}", unique = true)})
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvoiceNumberRange {

    @Id
    private String id;

    private String ubn;

    /**
     * Example: 1090910
     */
    private String rangeIdentifier;

    private List<NumberRange> numberRanges = new ArrayList<>();

    public InvoiceNumberRange(String ubn, String rangeIdentifier, String prefix, String rangeFrom, String rangeTo) {
        this.ubn = ubn;
        this.rangeIdentifier = rangeIdentifier;

        this.addNumberRange(prefix, rangeFrom, rangeTo);
    }

    public void addNumberRange(String prefix, String rangeFrom, String rangeTo) {

        final NumberRange numberRange = new NumberRange(prefix, rangeFrom, rangeTo);
        numberRanges.add(numberRange);
    }

    public NumberRange findDispensableNumberRange() {
        return numberRanges.stream()
                .filter(r -> !r.isFinished())
                .findFirst().orElseThrow(() -> {
                    throw new RuntimeException("There is no dispensable number range");
                });
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
                    throw new RuntimeException("There is no dispensable number range");
                });
    }

    @Data
    public static class NumberRange {

        /**
         * Example: AW-
         */
        private String prefix;

        private String rangeFrom;

        private String rangeTo;

        private int currentIncrement;

        private boolean started;

        /**
         * indicate if all numbers have been issued.
         */
        private boolean finished;

        public NumberRange(String prefix, String rangeFrom, String rangeTo) {
            this.prefix = prefix;
            this.rangeFrom = rangeFrom;
            this.rangeTo = rangeTo;
            this.currentIncrement = Integer.parseInt(rangeFrom) - 1;
        }

        public int getRemainingNumberInRange() {
            return Integer.parseInt(rangeTo) - currentIncrement;
        }

        public boolean isLastNumberInRange() {
            return currentIncrement + 1 == Integer.parseInt(rangeTo);
        }
    }
}
