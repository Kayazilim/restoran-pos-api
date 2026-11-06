package com.restoran.pos.notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-Sent Events (SSE) ile mutfak bildirimi. Mutfak ekranı tek bir bağlantı açar; sunucu yalnızca
 * sipariş geldiğinde/değiştiğinde olay gönderir. Böylece ekranların sürekli "yeni sipariş var mı?" diye
 * sorgulaması (polling) ve bunun yarattığı sunucu yükü ortadan kalkar.
 */
@Component
public class SseKitchenNotifier implements KitchenNotifier {

    private static final Logger log = LoggerFactory.getLogger(SseKitchenNotifier.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong eventSequence = new AtomicLong();
    private final long heartbeatSeconds;
    private final long emitterTimeoutMillis;
    private ScheduledExecutorService heartbeatScheduler;

    public SseKitchenNotifier(@Value("${pos.sse.heartbeat-seconds}") long heartbeatSeconds,
                              @Value("${pos.sse.emitter-timeout-minutes}") long emitterTimeoutMinutes) {
        this.heartbeatSeconds = heartbeatSeconds;
        this.emitterTimeoutMillis = TimeUnit.MINUTES.toMillis(emitterTimeoutMinutes);
    }

    /** Bean başlatılırken: açık bağlantıları ara sunucularda kopmaması için canlı tutan zamanlayıcı kurulur. */
    @PostConstruct
    void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kitchen-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        log.info("Mutfak bildirim kanalı (SSE) hazır; heartbeat aralığı {} sn.", heartbeatSeconds);
    }

    /** Bean sonlandırılırken: zamanlayıcı durdurulur ve açık mutfak bağlantıları düzgünce kapatılır. */
    @PreDestroy
    void shutdown() {
        heartbeatScheduler.shutdownNow();
        int openConnections = emitters.size();
        emitters.forEach(this::completeQuietly);
        emitters.clear();
        log.info("Mutfak bildirim kanalı kapatıldı; {} bağlantı sonlandırıldı.", openConnections);
    }

    @Override
    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(emitterTimeoutMillis);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            completeQuietly(emitter);
        });
        emitter.onError(error -> emitters.remove(emitter));
        emitters.add(emitter);

        send(emitter, SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Mutfak bildirim kanalına bağlanıldı", "user", username),
                        MediaType.APPLICATION_JSON));
        log.debug("Mutfak ekranı bağlandı: {} (aktif bağlantı: {})", username, emitters.size());
        return emitter;
    }

    @Override
    public void publish(KitchenOrderEvent event) {
        String eventId = String.valueOf(eventSequence.incrementAndGet());
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event()
                    .id(eventId)
                    .name(event.type().getEventName())
                    .data(event.order(), MediaType.APPLICATION_JSON));
        }
        log.debug("Mutfağa '{}' olayı gönderildi (sipariş {}, {} ekran)",
                event.type().getEventName(), event.order().orderId(), emitters.size());
    }

    @Override
    public int activeSubscriberCount() {
        return emitters.size();
    }

    private void sendHeartbeat() {
        for (SseEmitter emitter : emitters) {
            send(emitter, SseEmitter.event().comment("heartbeat"));
        }
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            // İstemci bağlantıyı kapatmış; bağlantı listeden çıkarılır.
            emitters.remove(emitter);
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // Bağlantı zaten kapanmış.
        }
    }
}
