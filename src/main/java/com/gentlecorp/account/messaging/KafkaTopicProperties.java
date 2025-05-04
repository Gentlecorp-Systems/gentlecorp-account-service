package com.gentlecorp.account.messaging;

/**
 * Zentrale Konfiguration der Kafka-Topic-Namen.
 * <p>
 * Die Namen folgen dem Schema: {@code <service>.<entities>.<event>}.
 * </p>
 *
 * @author Caleb
 * @since 20.04.2025
 */
public final class KafkaTopicProperties {

    private KafkaTopicProperties() {
        // Utility class – private Konstruktor verhindert Instanziierung
    }

    /** ✉️ Mailversand bei Kundenregistrierung */
    public static final String TOPIC_NOTIFICATION_ACCOUNT_CREATED = "notification.account.created";

    public static final String TOPIC_NOTIFICATION_CUSTOMER_DELETED = "notification.account.deleted";

    /** ✉️ Kundenkonto wurde erstellt (Eingang) */
    public static final String TOPIC_CUSTOMER_CREATED = "account.customer.created";

    public static final String TOPIC_CUSTOMER_DELETED = "account.customer.deleted";


    public static final String TOPIC_ACTIVITY_EVENTS = "activity.account.log";

}
