package io.nextpos.einvoice.common.invoicenumber;

import com.mongodb.client.result.UpdateResult;
import io.nextpos.einvoice.common.shared.InvoiceObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.Collectors.groupingBy;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Transactional("mongoTx")
public class InvoiceNumberRangeServiceImpl implements InvoiceNumberRangeService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM");

    private final InvoiceNumberRangeRepository invoiceNumberRangeRepository;

    private final MongoTemplate mongoTemplate;

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Autowired
    public InvoiceNumberRangeServiceImpl(InvoiceNumberRangeRepository invoiceNumberRangeRepository, MongoTemplate mongoTemplate) {
        this.invoiceNumberRangeRepository = invoiceNumberRangeRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public InvoiceNumberRange saveInvoiceNumberRange(InvoiceNumberRange invoiceNumberRange) {
        return invoiceNumberRangeRepository.save(invoiceNumberRange);
    }

    @Override
    public InvoiceNumberRange getInvoiceNumberRange(String id) {
        return invoiceNumberRangeRepository.findById(id).orElseThrow(() -> {
            throw new InvoiceObjectNotFoundException(InvoiceNumberRange.class, id);
        });
    }


    @Override
    public boolean hasCurrentInvoiceNumberRange(String ubn) {
        return invoiceNumberRangeRepository.findByUbnAndRangeIdentifier(ubn, getCurrentRangeIdentifier()).isPresent();
    }

    @Override
    public InvoiceNumberRange getCurrentInvoiceNumberRange(String ubn) {
        return this.getInvoiceNumberRangeByRangeIdentifier(ubn, getCurrentRangeIdentifier());
    }

    @Override
    public InvoiceNumberRange getInvoiceNumberRangeByRangeIdentifier(String ubn, String rangeIdentifier) {

        return invoiceNumberRangeRepository.findByUbnAndRangeIdentifier(ubn, rangeIdentifier).orElseThrow(() -> {
            final String message = String.format("Invoice number range cannot be found: ubn=%s, rangeIdentifier=%s", ubn, rangeIdentifier);
            throw new InvoiceObjectNotFoundException(message);
        });
    }

    @Override
    public List<InvoiceNumberRange> getInvoiceNumberRanges(String ubn) {
        return invoiceNumberRangeRepository.findAllByUbnOrderByRangeIdentifier(ubn);
    }

    @Override
    public Map<String, List<InvoiceNumberRange>> getRecentInvoiceNumberRanges() {

        List<String> rangeIdentifiers = new ArrayList<>();
        rangeIdentifiers.add(getLastRangeIdentifier());
        rangeIdentifiers.add(getCurrentRangeIdentifier());
        rangeIdentifiers.add(getNextRangeIdentifier());

        return invoiceNumberRangeRepository.findAllByRangeIdentifierInOrderByRangeIdentifier(rangeIdentifiers).stream()
                .collect(groupingBy(InvoiceNumberRange::getUbn));
    }

    @Override
    public List<InvoiceNumberRange> getInvoiceNumberRangesByLastRangeIdentifier() {

        return invoiceNumberRangeRepository.findAllByRangeIdentifierAndStatus(getLastRangeIdentifier(), InvoiceNumberRange.InvoiceNumberRangeStatus.ACTIVE);
    }

    @Override
    public InvoiceNumberRange disableOneInvoiceNumberRange(String ubn, String rangeIdentifier, String rangeFrom) {

        final InvoiceNumberRange invoiceNumberRange = this.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);
        invoiceNumberRange.disableNumberRangeById(rangeFrom);

        return invoiceNumberRangeRepository.save(invoiceNumberRange);
    }

    @Override
    public InvoiceNumberRange deleteOneInvoiceNumberRange(String ubn, String rangeIdentifier, String rangeFrom) {

        final InvoiceNumberRange invoiceNumberRange = this.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);
        invoiceNumberRange.deleteNumberRangeById(rangeFrom);

        return invoiceNumberRangeRepository.save(invoiceNumberRange);
    }

    @Override
    public void deleteInvoiceNumberRange(String ubn, String rangeIdentifier) {

        final InvoiceNumberRange invoiceNumberRange = this.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);

        invoiceNumberRangeRepository.delete(invoiceNumberRange);
    }

    @Override
    public String resolveInvoiceNumber(String ubn) {
        Lock lock = acquireLock(ubn);

        try {
            lock.lock();
            final InvoiceNumberRange invoiceNumberRange = this.getCurrentInvoiceNumberRange(ubn);
            final InvoiceNumberRange.NumberRange dispensableNumberRange = invoiceNumberRange.findDispensableNumberRange();

            final Update updateOperation = new Update().inc("numberRanges.$.currentIncrement", 1);

            if (!dispensableNumberRange.isStarted()) {
                updateOperation.set("numberRanges.$.started", true);
            }

            if (dispensableNumberRange.isLastNumberInRange()) {
                updateOperation.set("numberRanges.$.finished", true);
            }

            final UpdateResult result = mongoTemplate.updateFirst(
                    Query.query(where("_id").is(invoiceNumberRange.getId()).and("numberRanges.rangeFrom").is(dispensableNumberRange.getRangeFrom())),
                    updateOperation,
                    InvoiceNumberRange.class);

            if (result.getModifiedCount() != 1) {
                throw new RuntimeException("Number increment operation failed");
            }

            final InvoiceNumberRange updatedInvoiceNumberRange = this.getInvoiceNumberRange(invoiceNumberRange.getId());
            final InvoiceNumberRange.NumberRange dispenserNumberRange = updatedInvoiceNumberRange.findNumberRangeById(dispensableNumberRange.getRangeFrom());

            NumberStyleFormatter formatter = new NumberStyleFormatter("00000000");
            final String number = formatter.getNumberFormat(Locale.getDefault()).format(dispenserNumberRange.getCurrentIncrement());
            return dispenserNumberRange.getPrefix() + "-" + number;

        } finally {
            lock.unlock();
        }
    }

    private Lock acquireLock(String ubn) {
        return locks.computeIfAbsent(ubn, s -> new ReentrantLock());
    }

    @Override
    public String getCurrentRangeIdentifier() {
        return getRangeIdentifier(YearMonth.now());
    }

    private String getLastRangeIdentifier() {
        int monthToSubtract = YearMonth.now().getMonthValue() % 2 == 0 ? 2 : 1;
        return getRangeIdentifier(YearMonth.now().minusMonths(monthToSubtract));
    }

    private String getNextRangeIdentifier() {
        int monthToAdd = YearMonth.now().getMonthValue() % 2 == 0 ? 2 : 1;
        return getRangeIdentifier(YearMonth.now().plusMonths(monthToAdd));
    }


    @Override
    public String getRangeIdentifier(YearMonth yearMonth) {

        StringBuilder rangeIdentifier = new StringBuilder();
        rangeIdentifier.append(MinguoDate.from(yearMonth.atDay(1)).get(ChronoField.YEAR));

        if (yearMonth.getMonthValue() % 2 == 0) {
            rangeIdentifier.append(yearMonth.minusMonths(1).format(FORMATTER)).append(yearMonth.format(FORMATTER));
        } else {
            rangeIdentifier.append(yearMonth.format(FORMATTER)).append(yearMonth.plusMonths(1).format(FORMATTER));
        }

        return rangeIdentifier.toString();
    }
}
