package io.nextpos.einvoice.common.encryption;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EncryptionServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImplTest.class);

    private final EncryptionService encryptionService;

    @Autowired
    EncryptionServiceImplTest(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Test
    void generateAESKey() {

        LOGGER.info("{}", encryptionService.generateAESKey("90rainapp"));
    }
}