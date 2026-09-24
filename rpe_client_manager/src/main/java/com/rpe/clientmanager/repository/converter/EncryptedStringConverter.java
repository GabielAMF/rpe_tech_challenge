package com.rpe.clientmanager.repository.converter;

import com.rpe.clientmanager.service.PayloadCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Encrypts a column on write and decrypts it on read, so the entity holds plain values and the database only
 * ever sees ciphertext. A Spring bean: Hibernate gets it (with its PayloadCipher) from the Spring context.
 */
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final PayloadCipher cipher;

    @Override
    public String convertToDatabaseColumn(String value) {
        return value == null ? null : cipher.encrypt(value);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : cipher.decrypt(dbData);
    }
}
