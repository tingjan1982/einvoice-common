package io.nextpos.einvoice.common.encryption;

public interface EncryptionService {

    String generateAESKey(String password);
}
