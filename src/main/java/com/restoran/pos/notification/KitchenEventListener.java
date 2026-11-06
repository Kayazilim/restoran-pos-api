package com.restoran.pos.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sipariş olaylarını yalnızca veritabanı işlemi başarıyla commit edildikten sonra mutfağa iletir;
 * geri alınan (ör. yetersiz stok) siparişler mutfağa hiç düşmez.
 */
@Component
public class KitchenEventListener {

    private final KitchenNotifier kitchenNotifier;

    public KitchenEventListener(KitchenNotifier kitchenNotifier) {
        this.kitchenNotifier = kitchenNotifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onKitchenOrderEvent(KitchenOrderEvent event) {
        kitchenNotifier.publish(event);
    }
}
