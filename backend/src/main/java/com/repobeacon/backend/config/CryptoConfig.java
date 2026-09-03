package com.repobeacon.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;

@Configuration
public class CryptoConfig {

  @Bean
  BytesEncryptor tokenEncryptor(
      @Value("${app.token-encryptor-password}") String password,
      @Value("${app.token-encryptor-salt}") String salt) {
    return AesGcmBytesEncryptor.withPassword(password, salt).build();
  }
}
