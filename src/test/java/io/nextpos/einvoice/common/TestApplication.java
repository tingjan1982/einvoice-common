package io.nextpos.einvoice.common;

import com.mongodb.BasicDBList;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mongodb.MongoCollectionUtils;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = {InvoiceNumberRange.class, PendingEInvoiceQueue.class})
public class TestApplication {

    private static final byte[] IP4_LOOPBACK_ADDRESS = {127, 0, 0, 1};

    private static final byte[] IP6_LOOPBACK_ADDRESS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private final MongoProperties properties;

    @Autowired
    public TestApplication(MongoProperties properties) {
        this.properties = properties;
    }

    @Bean("mongoTx")
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    /**
     * Override the configuration in EmbeddedMongoAutoConfiguration to start Mongo server with replica set and journal.
     */
    @Bean
    public IMongodConfig embeddedMongoConfiguration(EmbeddedMongoProperties embeddedProperties) throws IOException {
        IMongoCmdOptions cmdOptions = new MongoCmdOptionsBuilder()
                .useNoJournal(false)
                .build();

        MongodConfigBuilder builder = new MongodConfigBuilder().version(determineVersion(embeddedProperties)).cmdOptions(cmdOptions);

        EmbeddedMongoProperties.Storage storage = embeddedProperties.getStorage();
        if (storage != null) {
            String databaseDir = storage.getDatabaseDir();
            String replSetName = storage.getReplSetName();
            int oplogSize = (storage.getOplogSize() != null) ? (int) storage.getOplogSize().toMegabytes() : 0;
            builder.replication(new Storage(databaseDir, replSetName, oplogSize));
        }
        Integer configuredPort = this.properties.getPort();
        if (configuredPort != null && configuredPort > 0) {
            builder.net(new Net(getHost().getHostAddress(), configuredPort, Network.localhostIsIPv6()));
        } else {
            builder.net(new Net(getHost().getHostAddress(), Network.getFreeServerPort(getHost()),
                    Network.localhostIsIPv6()));
        }
        return builder.build();
    }

    private IFeatureAwareVersion determineVersion(EmbeddedMongoProperties embeddedProperties) {
        if (embeddedProperties.getFeatures() == null) {
            for (Version version : Version.values()) {
                if (version.asInDownloadPath().equals(embeddedProperties.getVersion())) {
                    return version;
                }
            }
            return Versions.withFeatures(new GenericVersion(embeddedProperties.getVersion()));
        }
        return Versions.withFeatures(new GenericVersion(embeddedProperties.getVersion()),
                embeddedProperties.getFeatures().toArray(new Feature[0]));
    }

    private InetAddress getHost() throws UnknownHostException {
        if (this.properties.getHost() == null) {
            return InetAddress.getByAddress(Network.localhostIsIPv6() ? IP6_LOOPBACK_ADDRESS : IP4_LOOPBACK_ADDRESS);
        }
        return InetAddress.getByName(this.properties.getHost());
    }

    @Bean
    public MongoInitializer mongoInitializer(MongoTemplate mongoTemplate, MongoClient mongoClient) {
        return new MongoInitializer(mongoTemplate, mongoClient);
    }

    static class MongoInitializer implements InitializingBean {

        private final MongoTemplate template;

        private final MongoClient mongoClient;

        MongoInitializer(MongoTemplate template, MongoClient mongoClient) {
            this.template = template;
            this.mongoClient = mongoClient;
        }

        /**
         * Create collections dynamically.
         */
        @Override
        public void afterPropertiesSet() {

            final ServerAddress address = mongoClient.getClusterDescription().getServerDescriptions().get(0).getAddress();
            BasicDBList members = new BasicDBList();
            members.add(new Document("_id", 0).append("host", address.getHost() + ":" + address.getPort()));
            Document config = new Document("_id", "rep0");
            config.put("members", members);
            MongoDatabase admin = mongoClient.getDatabase("admin");
            admin.runCommand(new Document("replSetInitiate", config));

            Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> {
                try (ClientSession session = mongoClient.startSession()) {
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            });

            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.data.mongodb.core.mapping.Document.class));

            scanner.findCandidateComponents("io.nextpos").forEach(b -> {
                try {
                    final String collection = MongoCollectionUtils.getPreferredCollectionName(Class.forName(b.getBeanClassName()));
                    this.template.createCollection(collection);

                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

}
