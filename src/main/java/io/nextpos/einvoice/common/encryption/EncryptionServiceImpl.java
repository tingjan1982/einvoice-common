package io.nextpos.einvoice.common.encryption;

import com.tradevan.geinv.kms.dist.DistKMSService;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    @Override
    public String generateAESKey(String password) {

        try {
            final Method getSecretKeyHex = ReflectionUtils.findMethod(DistKMSService.class, "getSecretKeyHex");
            ReflectionUtils.makeAccessible(Objects.requireNonNull(getSecretKeyHex));

            return (String) ReflectionUtils.invokeMethod(getSecretKeyHex, new DistKMSService(password));

        } catch (Exception e) {
            throw new RuntimeException("Error while generating aes key: " + e.getMessage(), e);
        }
    }
}
