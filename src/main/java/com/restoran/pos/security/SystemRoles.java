package com.restoran.pos.security;

import java.util.List;
import java.util.Set;

import static com.restoran.pos.security.Permissions.ACTUATOR_READ;
import static com.restoran.pos.security.Permissions.ALL_TABLES_ACCESS;
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
 * Proje önerisindeki beş temel rol ve bu rollerin yetki eşlemesi (rol → yetki).
 * Bu eşleme ilk açılışta veritabanına yazılır; yeni roller API üzerinden eklenebilir.
 */
public final class SystemRoles {

    public static final String MUSTERI = "MUSTERI";
    public static final String GARSON = "GARSON";
    public static final String KASIYER = "KASIYER";
    public static final String MUTFAK = "MUTFAK";
    public static final String YONETICI = "YONETICI";

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(MUSTERI,
                    "Masadaki QR kod ile masaya özel kimlik alır; yalnızca kendi masası için sipariş verir ve hesabını görür",
                    Set.of(MENU_READ, TABLE_READ, ORDER_READ, ORDER_WRITE, BILL_READ)),
            new Definition(GARSON,
                    "Kendi masalarının siparişlerini açar, düzenler ve hesabını görür",
                    Set.of(MENU_READ, TABLE_READ, ORDER_READ, ORDER_WRITE, ORDER_DELETE, BILL_READ, ASSISTANT_USE)),
            new Definition(KASIYER,
                    "Açık adisyonları görür, ödeme alır ve adisyonu kapatır",
                    Set.of(MENU_READ, TABLE_READ, ORDER_READ, BILL_READ, BILL_WRITE, ALL_TABLES_ACCESS, ASSISTANT_USE)),
            new Definition(MUTFAK,
                    "Yeni siparişleri görür ve hazırlık durumunu günceller; ödeme ve stok bilgisine erişemez",
                    Set.of(MENU_READ, KITCHEN_READ, KITCHEN_WRITE, ASSISTANT_USE)),
            new Definition(YONETICI,
                    "Ürün, stok, fiyat, masa ve kullanıcı yönetimini yapar, raporları görür",
                    Set.of(MENU_READ, PRODUCT_READ, PRODUCT_WRITE, PRODUCT_DELETE,
                            TABLE_READ, TABLE_WRITE, TABLE_DELETE, ORDER_READ, BILL_READ,
                            USER_READ, USER_WRITE, USER_DELETE, ROLES_READ, ROLES_WRITE, ROLES_DELETE,
                            REPORT_READ, KNOWLEDGE_READ, KNOWLEDGE_WRITE, KNOWLEDGE_DELETE,
                            ASSISTANT_USE, ACTUATOR_READ, ALL_TABLES_ACCESS)));

    private SystemRoles() {
    }

    public static boolean isSystemRole(String roleName) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.name().equals(roleName));
    }

    public record Definition(String name, String description, Set<String> permissions) {
    }
}
