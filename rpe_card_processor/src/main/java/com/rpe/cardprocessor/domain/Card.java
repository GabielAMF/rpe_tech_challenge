package com.rpe.cardprocessor.domain;

import com.rpe.cardprocessor.repository.converter.EncryptedStringConverter;
import com.rpe.cardprocessor.repository.converter.EncryptedYearMonthConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * A produced card. Number, expiry and CVV are sensitive: encrypted in the database (AES-GCM converters), never
 * logged, and never exposed; the API shows {@link #getMaskedNumber()} only. No Lombok {@code @ToString} on purpose.
 * One card per customer, and one card per card-production event (idempotency).
 */
@Getter
@Entity
@Table(name = "card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The {@code eventId} of the card production message this card was produced from. */
    @Column(name = "source_event_id", nullable = false, updatable = false)
    private UUID sourceEventId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "holder_name", nullable = false, length = 150)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 20)
    private CardOperator operator;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "number_encrypted", nullable = false)
    private String number;

    @Column(name = "masked_number", nullable = false, length = 19)
    private String maskedNumber;

    @Convert(converter = EncryptedYearMonthConverter.class)
    @Column(name = "expiry_encrypted", nullable = false)
    private YearMonth expiry;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "cvv_encrypted", nullable = false)
    private String cvv;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    @Embedded
    private CardProduct product;

    public Card(UUID sourceEventId, UUID customerId, String holderName, GeneratedCardData data, CardProduct product) {
        this.sourceEventId = Objects.requireNonNull(sourceEventId, "sourceEventId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.holderName = Objects.requireNonNull(holderName, "holderName").trim();
        this.operator = data.operator();
        this.number = data.number();
        this.maskedNumber = mask(data.number());
        this.expiry = data.expiry();
        this.cvv = data.cvv();
        this.product = Objects.requireNonNull(product, "product");
        this.status = CardStatus.ATIVO;
    }

    /** {@code **** **** **** 1234}: only the last four digits, the standard safe-to-show part. */
    private static String mask(String number) {
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
