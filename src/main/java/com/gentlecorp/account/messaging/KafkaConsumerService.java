package com.gentlecorp.account.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentlecorp.account.models.event.CreateAccountDTO;
import com.gentlecorp.account.models.event.DeleteAccountDTO;
import com.gentlecorp.account.models.inputs.CreateAccountInput;
import com.gentlecorp.account.models.mapper.AccountMapper;
import com.gentlecorp.account.service.AccountWriteService;
import com.gentlecorp.account.tracing.LoggerPlus;
import com.gentlecorp.account.tracing.LoggerPlusFactory;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.gentlecorp.account.messaging.KafkaTopicProperties.TOPIC_CUSTOMER_CREATED;
import static com.gentlecorp.account.messaging.KafkaTopicProperties.TOPIC_CUSTOMER_DELETED;

/**
 * Kafka-Consumer für eingehende Events zur Kontoerstellung und -löschung.
 *
 * <p>
 * Unterstützt folgende Nachrichtenformate:
 * <ul>
 *     <li>{@link CreateAccountInput} auf <code>account.customer.created</code></li>
 *     <li>{@link DeleteAccountDTO} auf <code>account.customer.deleted</code></li>
 * </ul>
 * </p>
 *
 * @author
 * @since 21.04.2025
 */
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AccountWriteService accountWriteService;
    private final AccountMapper accountMapper;
    private final Tracer tracer;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    /**
     * Konsumiert neue Kunden-Ereignisse und erstellt ein Konto.
     *
     * @param record Kafka-Record mit JSON-Payload vom Typ {@link CreateAccountInput}
     */
    @KafkaListener(topics = TOPIC_CUSTOMER_CREATED, groupId = "${app.groupId}")
    @Observed(name = "kafka-consume.account-customer-created")
    public void consumeCustomerCreated(ConsumerRecord<String, CreateAccountDTO> record) {
        final var headers = record.headers();
        final var dto = record.value();
        final var createAccountInput = accountMapper.toCreateInput(dto);

        // ✨ 1. Extrahiere traceparent Header (W3C) oder B3 als Fallback
        final var traceParent = getHeader(headers, "traceparent");

        SpanContext linkedContext = null;
        if (traceParent != null && traceParent.startsWith("00-")) {
            String[] parts = traceParent.split("-");
            if (parts.length == 4) {
                String traceId = parts[1];
                String spanId = parts[2];
                boolean sampled = "01".equals(parts[3]);

                linkedContext = SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
                    TraceState.getDefault()
                );
            }
        }

        // ✨ 2. Starte neuen Trace mit Link (nicht als Parent!)
        SpanBuilder spanBuilder = tracer.spanBuilder("kafka.account.consume")
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", TOPIC_CUSTOMER_CREATED)
            .setAttribute("messaging.operation", "consume");

        if (linkedContext != null && linkedContext.isValid()) {
            spanBuilder.addLink(linkedContext);
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            assert scope != null;
            logger().info("📥 Empfangene Nachricht auf '{}': {}", TOPIC_CUSTOMER_CREATED, dto);
            final var accountInput = accountMapper.toAccount(createAccountInput);
            final var account = accountWriteService.create(accountInput);
            logger().info("✅ Konto erstellt für Benutzername='{}', Account-ID={}", createAccountInput.username(), account.getId());
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Kafka-Fehler");
            logger().error("❌ Fehler beim Erstellen des Kontos", e);
        } finally {
            span.end();
        }
    }

    private String getHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    /**
     * Konsumiert Lösch-Events und entfernt die zugehörigen Accounts.
     *
     * @param record Kafka-Record mit JSON-Payload vom Typ {@link DeleteAccountDTO}
     */
    @Observed(name = "kafka-consume.account-customer-deleted")
    @KafkaListener(topics = TOPIC_CUSTOMER_DELETED, groupId = "${app.groupId}")
    public void consumeCustomerDeleted(ConsumerRecord<String, DeleteAccountDTO> record) {
        Headers headers = record.headers();
        final var deleteAccountDTO = record.value();

        // ✨ 1. Extrahiere traceparent Header (W3C) oder B3 als Fallback
        String traceParent = getHeader(headers, "traceparent");

        SpanContext linkedContext = null;
        if (traceParent != null && traceParent.startsWith("00-")) {
            String[] parts = traceParent.split("-");
            if (parts.length == 4) {
                String traceId = parts[1];
                String spanId = parts[2];
                boolean sampled = "01".equals(parts[3]);

                linkedContext = SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
                    TraceState.getDefault()
                );
            }
        }

        // ✨ 2. Starte neuen Trace mit Link (nicht als Parent!)
        SpanBuilder spanBuilder = tracer.spanBuilder("kafka.account.consume")
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", TOPIC_CUSTOMER_CREATED)
            .setAttribute("messaging.operation", "consume");

        if (linkedContext != null && linkedContext.isValid()) {
            spanBuilder.addLink(linkedContext);
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            assert scope != null;
            logger().info("📥 Empfangene Nachricht auf '{}': {}", TOPIC_CUSTOMER_DELETED, deleteAccountDTO);
            accountWriteService.deleteAccountByUsernameOrCustomerId(deleteAccountDTO.id(), deleteAccountDTO.username());
            logger().info("🗑️ Konto gelöscht: username='{}', customerId={}", deleteAccountDTO.username(), deleteAccountDTO.id());
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Kafka-Fehler");
            logger().error("❌ Fehler beim Erstellen des Kontos", e);
        } finally {
            span.end();
        }
    }
}
