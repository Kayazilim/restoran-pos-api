package com.restoran.pos.config;

import com.restoran.pos.security.RestAccessDeniedHandler;
import com.restoran.pos.security.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.restoran.pos.security.Permissions.ACTUATOR_READ;
import static com.restoran.pos.security.Permissions.ASSISTANT_USE;
import static com.restoran.pos.security.Permissions.BILL_READ;
import static com.restoran.pos.security.Permissions.BILL_WRITE;
import static com.restoran.pos.security.Permissions.KITCHEN_READ;
import static com.restoran.pos.security.Permissions.KITCHEN_WRITE;
import static com.restoran.pos.security.Permissions.KNOWLEDGE_DELETE;
import static com.restoran.pos.security.Permissions.KNOWLEDGE_READ;
import static com.restoran.pos.security.Permissions.KNOWLEDGE_WRITE;
import static com.restoran.pos.security.Permissions.MENU_READ;
import static com.restoran.pos.security.Permissions.ORDER_DELETE;
import static com.restoran.pos.security.Permissions.ORDER_READ;
import static com.restoran.pos.security.Permissions.ORDER_WRITE;
import static com.restoran.pos.security.Permissions.PRODUCT_DELETE;
import static com.restoran.pos.security.Permissions.PRODUCT_READ;
import static com.restoran.pos.security.Permissions.PRODUCT_WRITE;
import static com.restoran.pos.security.Permissions.REPORT_READ;
import static com.restoran.pos.security.Permissions.ROLES_DELETE;
import static com.restoran.pos.security.Permissions.ROLES_READ;
import static com.restoran.pos.security.Permissions.ROLES_WRITE;
import static com.restoran.pos.security.Permissions.TABLE_DELETE;
import static com.restoran.pos.security.Permissions.TABLE_READ;
import static com.restoran.pos.security.Permissions.TABLE_WRITE;
import static com.restoran.pos.security.Permissions.USER_DELETE;
import static com.restoran.pos.security.Permissions.USER_READ;
import static com.restoran.pos.security.Permissions.USER_WRITE;

/**
 * HTTP Basic kimlik doğrulaması ve HTTP metodu + uç nokta yolu üzerinden yetkilendirme.
 * Okuma (READ), ekleme/güncelleme (WRITE; PUT ve PATCH dahil) ve silme (DELETE) ayrı yetki düzeyleridir.
 * Ek olarak controller metotları @PreAuthorize ile (metot güvenliği), kayıt sahipliği ise servis katmanında korunur.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Kimlik doğrulaması gerektirmeyen uç noktalar: API dokümantasyonu ile temel sağlık/bilgi uç noktaları. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/error"
    };

    // Argon2id parametreleri: 16 bayt salt, 32 bayt hash, 1 iş parçacığı, 16 MB bellek, 2 iterasyon.
    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_MEMORY_KB = 1 << 14;
    private static final int ARGON2_ITERATIONS = 2;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // Durumsuz (stateless) REST API: oturum çerezi yok, her istek HTTP Basic ile kimlik doğrular.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // SSE akışının asenkron devamı ve hata yönlendirmeleri, ilk istekte zaten yetkilendirilmiştir.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Yönetim bilgisi sunan diğer Actuator uç noktaları (ör. metrics) yalnızca yöneticiye açık
                        .requestMatchers("/actuator/**").hasAuthority(ACTUATOR_READ)

                        // Menü: tüm roller görür
                        .requestMatchers(HttpMethod.GET, "/api/menu/**").hasAuthority(MENU_READ)

                        // Ürün, fiyat ve stok: yalnızca yönetici
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAuthority(PRODUCT_READ)
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority(PRODUCT_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority(PRODUCT_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasAuthority(PRODUCT_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority(PRODUCT_DELETE)

                        // Masalar
                        .requestMatchers(HttpMethod.GET, "/api/tables/**").hasAuthority(TABLE_READ)
                        .requestMatchers(HttpMethod.POST, "/api/tables/**").hasAuthority(TABLE_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/tables/**").hasAuthority(TABLE_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/tables/**").hasAuthority(TABLE_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/tables/**").hasAuthority(TABLE_DELETE)

                        // Siparişler
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAuthority(ORDER_READ)
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAuthority(ORDER_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAuthority(ORDER_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasAuthority(ORDER_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAuthority(ORDER_DELETE)

                        // Mutfak: fiyat ve stok içermeyen sipariş görünümü, durum güncelleme ve SSE akışı
                        .requestMatchers(HttpMethod.GET, "/api/kitchen/**").hasAuthority(KITCHEN_READ)
                        .requestMatchers(HttpMethod.PATCH, "/api/kitchen/**").hasAuthority(KITCHEN_WRITE)

                        // Adisyon ve ödeme
                        .requestMatchers(HttpMethod.GET, "/api/bills/**").hasAuthority(BILL_READ)
                        .requestMatchers(HttpMethod.POST, "/api/bills/**").hasAuthority(BILL_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/bills/**").hasAuthority(BILL_WRITE)

                        // Kullanıcılar ve rol atamaları: yalnızca yönetici
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAuthority(USER_READ)
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAuthority(USER_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority(USER_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasAuthority(USER_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority(USER_DELETE)

                        // Roller ve yetkiler (genişletilebilir rol yapısı)
                        .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/permissions/**").hasAuthority(ROLES_READ)
                        .requestMatchers(HttpMethod.POST, "/api/roles/**").hasAuthority(ROLES_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/roles/**").hasAuthority(ROLES_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/roles/**").hasAuthority(ROLES_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasAuthority(ROLES_DELETE)

                        // Raporlar
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAuthority(REPORT_READ)

                        // Asistan ve asistanın referans belgeleri
                        .requestMatchers(HttpMethod.POST, "/api/assistant/questions").hasAuthority(ASSISTANT_USE)
                        .requestMatchers(HttpMethod.GET, "/api/knowledge-documents/**").hasAuthority(KNOWLEDGE_READ)
                        .requestMatchers(HttpMethod.POST, "/api/knowledge-documents/**").hasAuthority(KNOWLEDGE_WRITE)
                        .requestMatchers(HttpMethod.PUT, "/api/knowledge-documents/**").hasAuthority(KNOWLEDGE_WRITE)
                        .requestMatchers(HttpMethod.PATCH, "/api/knowledge-documents/**").hasAuthority(KNOWLEDGE_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/knowledge-documents/**").hasAuthority(KNOWLEDGE_DELETE)

                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * Parolalar Argon2id ile hash'lenir. Üretilen değer "$argon2id$v=19$m=16384,t=2,p=1$..." biçimindedir
     * ve tamamı users.password_hash (255 karakter) sütununda saklanır.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(ARGON2_SALT_LENGTH, ARGON2_HASH_LENGTH, ARGON2_PARALLELISM,
                ARGON2_MEMORY_KB, ARGON2_ITERATIONS);
    }
}
