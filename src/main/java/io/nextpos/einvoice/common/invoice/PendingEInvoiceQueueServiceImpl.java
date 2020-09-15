package io.nextpos.einvoice.common.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PendingEInvoiceQueueServiceImpl implements PendingEInvoiceQueueService {

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public PendingEInvoiceQueueServiceImpl(PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, MongoTemplate mongoTemplate) {
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public PendingEInvoiceQueue savePendingEInvoiceQueue(ElectronicInvoice electronicInvoice) {

        PendingEInvoiceQueue pendingEInvoice = new PendingEInvoiceQueue(electronicInvoice.getId(),
                electronicInvoice.getInvoiceNumber(),
                electronicInvoice.getSellerUbn());

        return pendingEInvoiceQueueRepository.save(pendingEInvoice);
    }

    @Override
    public List<PendingEInvoiceQueue> findPendingEInvoicesByUbn(String ubn) {
        return pendingEInvoiceQueueRepository.findAllByUbnAndStatus(ubn, PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING);
    }

    @Override
    public List<PendingEInvoiceQueue> findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus status) {
        return pendingEInvoiceQueueRepository.findAllByStatus(status);
    }

    @Override
    public PendingInvoiceStats generatePendingEInvoiceStats() {

        final GroupOperation groupBy = Aggregation.group("ubn", "status")
                .count().as("invoiceCount");

        final TypedAggregation<PendingEInvoiceQueue> aggregation = Aggregation.newAggregation(PendingEInvoiceQueue.class, groupBy);

        final AggregationResults<PendingInvoiceStats> result = mongoTemplate.aggregate(aggregation, PendingInvoiceStats.class);

        return result.getUniqueMappedResult();
    }
}
