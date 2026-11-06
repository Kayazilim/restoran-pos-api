package com.restoran.pos.exception;

import java.util.List;

/**
 * Sipariş edilen adet mevcut stoktan fazlaysa veya ürün satışta değilse fırlatılır (409).
 * Bu durumda işlem geri alınır ve stok hiçbir şekilde eksiye düşmez.
 */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(List<String> problems) {
        super("Sipariş onaylanamadı: yetersiz stok veya satışta olmayan ürün var.", problems);
    }
}
