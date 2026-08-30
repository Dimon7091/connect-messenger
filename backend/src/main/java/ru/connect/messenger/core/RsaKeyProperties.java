package ru.connect.messenger.core;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "rsa")
@Setter
@Slf4j
public class RsaKeyProperties {
    private String privateKey;
    private String publicKey;
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;

    @PostConstruct
    public void init() throws Exception {
        if (privateKey != null) {
            String pemContent;
            // Если строка начинается с префикса, читаем файл
            if (privateKey.startsWith("classpath:") || privateKey.startsWith("file:")) {
                Resource resource = new DefaultResourceLoader().getResource(privateKey);
                pemContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                // Иначе считаем, что это сам ключ
                pemContent = privateKey;
            }
            this.rsaPrivateKey = convertPrivateKey(pemContent);
        }

        if (publicKey != null) {
            String pemContent;
            // Если строка начинается с префикса, читаем файл
            if (publicKey.startsWith("classpath:") || publicKey.startsWith("file:")) {
                Resource resource = new DefaultResourceLoader().getResource(publicKey);
                pemContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                // Иначе считаем, что это сам ключ
                pemContent = publicKey;
            }
            this.rsaPublicKey = convertPublicKey(pemContent);
        }
    }

    private RSAPrivateKey convertPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("[^A-Za-z0-9+/=]", "");

        // Дополняем до кратности 4
        int mod = cleaned.length() % 4;
        if (mod != 0) {
            cleaned += "=".repeat(4 - mod);
        }

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    private RSAPublicKey convertPublicKey(String pem) throws Exception {
        String cleaned = pem
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("[^A-Za-z0-9+/=]", "");

        // Дополняем до кратности 4
        int mod = cleaned.length() % 4;
        if (mod != 0) {
            cleaned += "=".repeat(4 - mod);
        }

        byte[] decoded = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    // Геттеры для использования в других бинах
    public RSAPrivateKey getPrivateKey() {
        return rsaPrivateKey;
    }

    public RSAPublicKey getPublicKey() {
        return rsaPublicKey;
    }
}
