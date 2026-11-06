package com.restoran.pos.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sistemdeki yetki (authority) adları. Her kaynak için okuma, ekleme/güncelleme ve silme
 * ayrı yetki düzeyleriyle korunur; PATCH de güncelleme (WRITE) yetkisi gerektirir.
 */
public final class Permissions {

    public static final String MENU_READ = "MENU_READ";

    public static final String PRODUCT_READ = "PRODUCT_READ";
    public static final String PRODUCT_WRITE = "PRODUCT_WRITE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";

    public static final String TABLE_READ = "TABLE_READ";
    public static final String TABLE_WRITE = "TABLE_WRITE";
    public static final String TABLE_DELETE = "TABLE_DELETE";

    public static final String ORDER_READ = "ORDER_READ";
    public static final String ORDER_WRITE = "ORDER_WRITE";
    public static final String ORDER_DELETE = "ORDER_DELETE";

    public static final String KITCHEN_READ = "KITCHEN_READ";
    public static final String KITCHEN_WRITE = "KITCHEN_WRITE";

    public static final String BILL_READ = "BILL_READ";
    public static final String BILL_WRITE = "BILL_WRITE";

    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";

    // "ROLE_" önekiyle başlamaması için ROLES_ kullanılır (Spring Security o öneki rollere ayırır).
    public static final String ROLES_READ = "ROLES_READ";
    public static final String ROLES_WRITE = "ROLES_WRITE";
    public static final String ROLES_DELETE = "ROLES_DELETE";

    public static final String REPORT_READ = "REPORT_READ";

    public static final String KNOWLEDGE_READ = "KNOWLEDGE_READ";
    public static final String KNOWLEDGE_WRITE = "KNOWLEDGE_WRITE";
    public static final String KNOWLEDGE_DELETE = "KNOWLEDGE_DELETE";

    public static final String ASSISTANT_USE = "ASSISTANT_USE";

    public static final String ACTUATOR_READ = "ACTUATOR_READ";

    /** Kayıt sahipliği kısıtı olmadan tüm masaların sipariş ve adisyonlarına erişim (kasiyer, yönetici). */
    public static final String ALL_TABLES_ACCESS = "ALL_TABLES_ACCESS";

    /** Yetki adı ve açıklaması; ilk açılışta veritabanına yüklenir. */
    public static final Map<String, String> DESCRIPTIONS = createDescriptions();

    private Permissions() {
    }

    private static Map<String, String> createDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put(MENU_READ, "Menüyü görüntüleme");
        descriptions.put(PRODUCT_READ, "Ürünleri stok bilgisiyle görüntüleme");
        descriptions.put(PRODUCT_WRITE, "Ürün ekleme ve güncelleme (fiyat ve stok dahil)");
        descriptions.put(PRODUCT_DELETE, "Ürün silme");
        descriptions.put(TABLE_READ, "Masaları görüntüleme (kayıt sahipliğine göre)");
        descriptions.put(TABLE_WRITE, "Masa ekleme, güncelleme ve QR kodu yenileme");
        descriptions.put(TABLE_DELETE, "Masa silme");
        descriptions.put(ORDER_READ, "Siparişleri görüntüleme (kayıt sahipliğine göre)");
        descriptions.put(ORDER_WRITE, "Sipariş verme ve düzenleme");
        descriptions.put(ORDER_DELETE, "Sipariş iptal etme");
        descriptions.put(KITCHEN_READ, "Mutfak sipariş ekranı ve anlık bildirimler");
        descriptions.put(KITCHEN_WRITE, "Siparişin hazırlık durumunu güncelleme");
        descriptions.put(BILL_READ, "Adisyonları görüntüleme (kayıt sahipliğine göre)");
        descriptions.put(BILL_WRITE, "Ödeme alma ve adisyon kapatma");
        descriptions.put(USER_READ, "Kullanıcıları görüntüleme");
        descriptions.put(USER_WRITE, "Kullanıcı ekleme, güncelleme ve rol atama");
        descriptions.put(USER_DELETE, "Kullanıcı silme");
        descriptions.put(ROLES_READ, "Rolleri ve yetkileri görüntüleme");
        descriptions.put(ROLES_WRITE, "Yeni rol tanımlama ve rol güncelleme");
        descriptions.put(ROLES_DELETE, "Rol silme");
        descriptions.put(REPORT_READ, "Satış raporlarını görüntüleme");
        descriptions.put(KNOWLEDGE_READ, "Asistan belgelerini görüntüleme");
        descriptions.put(KNOWLEDGE_WRITE, "Asistan belgesi yükleme ve güncelleme");
        descriptions.put(KNOWLEDGE_DELETE, "Asistan belgesi silme");
        descriptions.put(ASSISTANT_USE, "Personel asistanına soru sorma");
        descriptions.put(ACTUATOR_READ, "Actuator yönetim uç noktalarına erişim");
        descriptions.put(ALL_TABLES_ACCESS, "Tüm masaların kayıtlarına erişim (sahiplik kısıtı olmadan)");
        return Collections.unmodifiableMap(descriptions);
    }
}
