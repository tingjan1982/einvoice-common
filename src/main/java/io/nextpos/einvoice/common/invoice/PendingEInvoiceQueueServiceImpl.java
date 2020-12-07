package io.nextpos.einvoice.common.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional("mongoTx")
public class PendingEInvoiceQueueServiceImpl implements PendingEInvoiceQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingEInvoiceQueueServiceImpl.class);

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public PendingEInvoiceQueueServiceImpl(PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, MongoTemplate mongoTemplate) {
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public PendingEInvoiceQueue createPendingEInvoiceQueue(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType pendingEInvoiceType) {

        PendingEInvoiceQueue pendingEInvoice = new PendingEInvoiceQueue(electronicInvoice, pendingEInvoiceType);
        pendingEInvoiceQueueRepository.save(pendingEInvoice);

        LOGGER.info("Created pending e-invoice[type={}]: {}", pendingEInvoiceType, pendingEInvoice.getId());

        return pendingEInvoice;
    }

    @Override
    public PendingEInvoiceQueue updatePendingEInvoiceQueue(PendingEInvoiceQueue pendingEInvoiceQueue) {
        return pendingEInvoiceQueueRepository.save(pendingEInvoiceQueue);
    }

    @Override
    public List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn) {
        return pendingEInvoiceQueueRepository.findAllByUbnAndStatus(ubn, PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING);
    }

    @Override
    public List<PendingEInvoiceQueue> findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus... status) {
        return pendingEInvoiceQueueRepository.findAllByStatusIn(Arrays.asList(status));
    }

    @Override
    public void deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status) {
        pendingEInvoiceQueueRepository.deleteAllByStatus(status);
    }

    @Override
    public Map<String, List<PendingInvoiceStats>> generatePendingEInvoiceStats() {

        final GroupOperation groupBy = Aggregation.group("ubn", "status")
                .first("ubn").as("ubn")
                .first("status").as("status")
                .count().as("invoiceCount");

        final TypedAggregation<PendingEInvoiceQueue> aggregation = Aggregation.newAggregation(PendingEInvoiceQueue.class, groupBy);

        final AggregationResults<PendingInvoiceStats> result = mongoTemplate.aggregate(aggregation, PendingInvoiceStats.class);

        return result.getMappedResults().stream()
                .collect(groupingBy(PendingInvoiceStats::getUbn));
    }
}
