package com.restoran.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Uygulama genelinde kullanılan yardımcı bean tanımları.
 */
@Configuration
public class AppConfig {

    /** İş kurallarındaki zaman damgaları (adisyon açılış/kapanış, ödeme, hata modeli) için tek saat kaynağı. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Masa QR anahtarlarının üretiminde kullanılan kriptografik rastgele sayı üreteci. */
    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
