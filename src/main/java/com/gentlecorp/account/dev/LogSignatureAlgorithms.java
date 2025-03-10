package com.gentlecorp.account.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

public interface LogSignatureAlgorithms {
  @Bean
  @Profile("logSecurity")
  default ApplicationListener<ApplicationReadyEvent> logSignatureAlgorithms() {
    final var log = LoggerFactory.getLogger(LogSignatureAlgorithms.class);
    return event -> Arrays
      .stream(Security.getProviders())
      .forEach(provider -> logSignatureAlgorithms(provider, log));
  }

  private void logSignatureAlgorithms(final Provider provider, final Logger log) {
    provider
      .getServices()
      .forEach(service -> {
        if ("Signature".contentEquals(service.getType())) {
          log.debug("{}", service.getAlgorithm());
        }
      });
  }
}
