package com.gentlecorp.account.tracing;

import com.gentlecorp.account.messaging.KafkaPublisherService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Erweiterter Logger mit Kafka-Unterstützung.
 * <p>
 * Loggt gleichzeitig in die Konsole (Slf4j) und an einen zentralen Logging-Service via Kafka.
 * </p>
 */
@RequiredArgsConstructor
public class LoggerPlus {

    private final Logger logger;
    private final String serviceName;
    private final KafkaPublisherService kafkaPublisherService;
    private final Class<?> clazz;

    public static LoggerPlus of(Class<?> clazz, final KafkaPublisherService publisher, final String serviceName) {
        return new LoggerPlus(LoggerFactory.getLogger(clazz), serviceName, publisher, clazz);
    }

    private String getCallerContext() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(frames -> frames
                .filter(f -> f.getDeclaringClass().equals(clazz))
                .findFirst()
                .map(frame -> String.format("%s#%s",clazz.getSimpleName(), frame.getMethodName()))
                .orElse(clazz.getSimpleName()));
    }

    private void sendLog(String level, String message) {
        var context = getCallerContext();
        kafkaPublisherService.log(level, message, serviceName, context);
    }

    public void debug(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        logger.debug(msg);
//        sendLog("DEBUG", msg);
    }

    public void info(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        logger.info(msg);
        sendLog("INFO", msg);
    }

    public void warn(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        logger.warn(msg);
        sendLog("WARN", msg);
    }

    public void error(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        logger.error(msg);
        sendLog("ERROR", msg);
    }

    public void trace(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        logger.trace(msg);
//        sendLog("TRACE", msg);
    }
}
