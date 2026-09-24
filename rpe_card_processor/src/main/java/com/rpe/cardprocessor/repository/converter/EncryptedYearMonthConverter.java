package com.rpe.cardprocessor.repository.converter;

import com.rpe.cardprocessor.service.CardCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/** Card expiry (month/year only), encrypted like {@link EncryptedStringConverter}. */
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedYearMonthConverter implements AttributeConverter<YearMonth, String> {

    private final CardCipher cipher;

    @Override
    public String convertToDatabaseColumn(YearMonth value) {
        return value == null ? null : cipher.encrypt(value.toString());
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return dbData == null ? null : YearMonth.parse(cipher.decrypt(dbData));
    }
}
