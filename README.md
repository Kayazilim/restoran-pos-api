# Restoran POS API

**Spring Boot ile Çok Kanallı Sipariş (POS) API'si** — Sunucu Uygulama ve Geliştirme dersi projesi.

Kafe ve restoranlarda yoğun saatlerde yaşanan eşzamanlılık krizlerini (son ürünün iki kişiye satılması, "Siparişi Ver"e arka arkaya basılınca çifte sipariş/çifte ücret) kesin olarak önleyen; rollere göre sınırlandırılmış, mutfağa sunucudan anlık bildirim gönderen ve yalnızca yüklenen belgelere dayanan bir personel asistanı sunan katmanlı REST API.

---

## İçindekiler

1. [Çözülen problemler](#1-çözülen-problemler)
2. [Teknolojiler](#2-teknolojiler)
3. [Gerekli yazılımlar](#3-gerekli-yazılımlar)
4. [Hızlı başlangıç (Docker)](#4-hızlı-başlangıç-docker)
5. [Yerel geliştirme](#5-yerel-geliştirme-mysql-dockerda-uygulama-terminalde)
6. [Veritabanı ve bağlantı ayarları](#6-veritabanı-ve-bağlantı-ayarları)
7. [Adresler (API, Swagger, Actuator)](#7-adresler)
8. [Örnek kullanıcılar](#8-örnek-kullanıcılar-ve-rolleri)
9. [Roller ve yetkiler](#9-roller-ve-yetkiler)
10. [Uç noktalar](#10-uç-noktalar)
11. [Eşzamanlılık çözümü](#11-eşzamanlılık-çözümü)
12. [Mutfak bildirimi (SSE)](#12-mutfak-bildirimi-sse)
13. [Personel asistanı (RAG)](#13-personel-asistanı-rag)
14. [Hata modeli](#14-hata-modeli)
15. [Testler](#15-testler)
16. [Postman koleksiyonu](#16-postman-koleksiyonu)
17. [Proje yapısı](#17-proje-yapısı)
18. [Teknik gereklilikler kontrol listesi](#18-teknik-gereklilikler-kontrol-listesi)

---

## 1. Çözülen problemler

| Problem | Çözüm |
|---|---|
| **Son ürünü iki kişi aynı anda sipariş ediyor**, stok eksiye düşüyor | Ürün satırları `SELECT ... FOR UPDATE` (JPA `PESSIMISTIC_WRITE`) ile **id sırasıyla** kilitlenir. İkinci işlem bekler, güncel stoğu görür ve `409 Conflict` alır. Stok asla eksiye düşmez. |
| **"Siparişi Ver"e arka arkaya basılıyor**, sipariş/ücret iki kez işleniyor | Sipariş ve ödeme isteklerinde zorunlu `Idempotency-Key` başlığı vardır. Aynı anahtarla gelen tekrarlar yeniden işlenmez; önceki kayıt `200` ve `Idempotent-Replayed: true` başlığıyla döner. Veritabanındaki `UNIQUE` kısıt da ek bir güvencedir. |
| **Yoğun saatlerde sistem kilitleniyor** | Bütün işlemler kilitleri aynı sırada alır (masa → sipariş → ürünler → adisyon), bu yüzden kilitlenme (deadlock) oluşmaz. Kilit bekleme süresi sınırlıdır (`innodb_lock_wait_timeout=5`). Süre dolarsa istek beklemede kalmaz, anlaşılır bir `409` ile sonlanır. |
| **Yetki ve erişim karmaşası** | HTTP Basic kimlik doğrulaması kullanılır. Rol → yetki eşlemesi veritabanında tutulur. Yetkilendirme HTTP metodu ve yol üzerinden (`SecurityFilterChain`) ve metot güvenliğiyle (`@PreAuthorize`) yapılır. Kayıt sahipliği denetimi sayesinde garson yalnızca kendi masalarına, müşteri yalnızca kendi masasına erişir. |
| **Mutfak ekranları sürekli "yeni sipariş var mı?" diye soruyor** | Server-Sent Events (SSE) kullanılır: mutfak tek bir bağlantı açar, sunucu yalnızca sipariş geldiğinde ya da değiştiğinde haber verir. |
| **Akıllı asistan alerjen bilgisini yanlış veriyor** | Spring AI ile RAG kullanılır: cevaplar yalnızca yüklenen menü ve alerjen belgelerinden üretilir. Belgelerde dayanak yoksa model hiç çağrılmaz ve "belgelerde yok" cevabı döner. Sıcaklık 0'dır ve kaynak belgeler cevapta gösterilir. |

## 2. Teknolojiler

| Bileşen | Sürüm / Not |
|---|---|
| Java | 21 |
| Spring Boot | **4.1.2** |
| Kalıcılık | JPA (Jakarta Persistence 3.2) + Hibernate 7, `EntityManager` ile **elle yazılmış repository'ler** (Spring Data JPA/REST **kullanılmaz**) |
| Veritabanı | MySQL 8.4 |
| Güvenlik | Spring Security 7, HTTP Basic, **Argon2id** (`Argon2PasswordEncoder`, BouncyCastle) |
| Dokümantasyon | springdoc-openapi 3.2.0 (OpenAPI 3 + Swagger UI) |
| Yapay zeka | Spring AI 2.0.2, Google Gemini (sohbet + `gemini-embedding-001`), `SimpleVectorStore` |
| İzleme | Spring Boot Actuator (health, info, metrics) |
| Test | JUnit 6, MockMvc, Spring Security Test, H2 (MySQL kipi) |
| Çalıştırma | Docker + Docker Compose, Maven Wrapper |

## 3. Gerekli yazılımlar

| Yazılım | Neden | Windows kurulumu |
|---|---|---|
| **JDK 21** | Derleme ve çalıştırma | `winget install --id EclipseAdoptium.Temurin.21.JDK -e` |
| **Docker Desktop** | MySQL ve uygulamayı konteynerde çalıştırma | `winget install --id Docker.DockerDesktop -e` (kurulumdan sonra bilgisayarı yeniden başlatın) |
| **Git** | Kaynak kodu indirme | `winget install --id Git.Git -e` |
| Postman (isteğe bağlı) | Hazır istek koleksiyonunu çalıştırma | `winget install --id Postman.Postman -e` |

> Maven kurmanız **gerekmez**: proje Maven Wrapper (`mvnw` / `mvnw.cmd`) ile gelir ve doğru Maven sürümünü kendisi indirir.
> MySQL'i ayrıca kurmanız da gerekmez; Docker Compose MySQL 8.4'ü otomatik başlatır.

## 4. Hızlı başlangıç (Docker)

```bash
git clone https://github.com/Kayazilim/restoran-pos-api.git
cd restoran-pos-api
docker compose up -d --build
```

İlk açılışta MySQL hazırlanır, şema oluşturulur ve örnek veriler (kullanıcılar, masalar, menü, asistan belgeleri) yüklenir. Hazır olduğunu kontrol etmek için:

```bash
curl http://localhost:8080/actuator/health
```

Durdurmak için `docker compose down` komutunu kullanın. Veriler `mysql-data` biriminde saklandığından yeniden başlatmada **korunur**. Verileri tamamen silmek isterseniz `docker compose down -v` kullanın.

**Gemini asistanı (isteğe bağlı):** [Google AI Studio](https://aistudio.google.com/apikey) üzerinden ücretsiz bir API anahtarı alın. `.env.example` dosyasını `.env` adıyla kopyalayın, `GEMINI_API_KEY=...` satırını doldurun ve `docker compose up -d` komutunu tekrar çalıştırın. Anahtar tanımlı değilse uygulama yine çalışır. Bu durumda asistan, yapay zeka kullanmadan belgelerden **birebir alıntı** yapan güvenli moda geçer.

## 5. Yerel geliştirme (MySQL Docker'da, uygulama terminalde)

```bash
docker compose up -d mysql
./mvnw spring-boot:run
```

Windows'ta `./mvnw` yerine `mvnw.cmd` kullanın. Gemini anahtarı vermek için PowerShell'de çalıştırmadan önce `$env:GEMINI_API_KEY="..."` komutunu girin.

Jar oluşturup çalıştırmak için:

```bash
./mvnw clean package
java -jar target/restoran-pos-api-1.0.0.jar
```

## 6. Veritabanı ve bağlantı ayarları

Uygulama verileri **MySQL**'de kalıcı olarak saklanır. Şema, ilk açılışta Hibernate tarafından oluşturulur (`ddl-auto=update`). Sonraki açılışlarda mevcut veriler korunur; örnek veriler yalnızca tablolar boşken yüklenir.

| Ortam değişkeni | Varsayılan | Açıklama |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/restoran_pos?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&sessionVariables=innodb_lock_wait_timeout=5` | JDBC adresi (Compose içinde host `mysql`) |
| `DB_USERNAME` | `pos_user` | Veritabanı kullanıcısı |
| `DB_PASSWORD` | `pos_pass` | Veritabanı parolası |
| `MYSQL_ROOT_PASSWORD` | `root_pass` | Yalnızca MySQL konteyneri için |
| `MYSQL_PORT` | `3306` | MySQL'in bilgisayarınızda açılacağı port (3306 doluysa ör. 3307) |
| `GEMINI_API_KEY` | *(boş)* | Gemini API anahtarı; boşsa asistan anahtar kelime modunda çalışır |
| `GEMINI_CHAT_MODEL` | `gemini-flash-lite-latest` | Sohbet modeli. Varsayılan, Google'ın her zaman güncel "lite" modele yönlendirdiği takma addır; sabit sürüm adları (ör. `gemini-2.5-flash`) zamanla yeni kullanıcılara kapatılabilir |
| `GEMINI_EMBEDDING_MODEL` | `gemini-embedding-001` | Embedding modeli |
| `SERVER_PORT` | `8080` | HTTP portu |
| `POS_QR_BASE_URL` | `http://localhost:8080/qr` | Masa QR kodlarına yazılan adresin başlangıcı |

**Kendi MySQL kurulumunuzu kullanacaksanız:**

```sql
CREATE DATABASE restoran_pos CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'pos_user'@'%' IDENTIFIED BY 'pos_pass';
GRANT ALL PRIVILEGES ON restoran_pos.* TO 'pos_user'@'%';
```

Uygulamaya özel ayarlar `application.yml` içinde `pos.*` altında bulunur ve sınıflara `@Value` ile constructor parametresi olarak verilir:

| Ayar | Değer | Kullanıldığı yer |
|---|---|---|
| `pos.order.max-quantity-per-item` | 20 | `OrderServiceImpl` |
| `pos.concurrency.lock-timeout-ms` | 5000 | Repository kilit sorguları |
| `pos.sse.heartbeat-seconds` / `emitter-timeout-minutes` | 25 / 30 | `SseKitchenNotifier` |
| `pos.qr.base-url` | — | `TableServiceImpl` |
| `pos.report.zone-id` | Europe/Istanbul | `ReportServiceImpl` |
| `pos.assistant.top-k` / `similarity-threshold` | 4 / 0.55 | `RagMenuAssistant` |

## 7. Adresler

| Ne | Adres | Erişim |
|---|---|---|
| API kök adresi | http://localhost:8080/api | HTTP Basic |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Herkese açık ("Authorize" ile giriş yapın) |
| OpenAPI çıktısı (JSON) | http://localhost:8080/v3/api-docs | Herkese açık |
| Sağlık | http://localhost:8080/actuator/health | Herkese açık (ayrıntılar yalnızca yöneticiye) |
| Uygulama bilgisi (ad, açıklama, sürüm) | http://localhost:8080/actuator/info | Herkese açık |
| Metrikler | http://localhost:8080/actuator/metrics | Yalnızca YÖNETİCİ (`ACTUATOR_READ`) |
| Mutfak bildirim akışı (SSE) | http://localhost:8080/api/kitchen/stream | MUTFAK |

## 8. Örnek kullanıcılar ve rolleri

| Kullanıcı adı | Parola | Rol | Açıklama |
|---|---|---|---|
| `yonetici` | `Yonetici123!` | YONETICI | Ürün, stok, fiyat, masa, kullanıcı ve rol yönetimi; raporlar |
| `garson1` | `Garson123!` | GARSON | Masa 1, 2, 3'ün garsonu |
| `garson2` | `Garson123!` | GARSON | Masa 4, 5, 6'nın garsonu |
| `kasiyer` | `Kasiyer123!` | KASIYER | Açık adisyonlar, ödeme alma, adisyon kapatma |
| `mutfak` | `Mutfak123!` | MUTFAK | Mutfak ekranı ve hazırlık durumu (fiyat/stok göremez) |
| `masa-1` … `masa-6` | `qr-masa-1` … `qr-masa-6` | MUSTERI | Masadaki QR kodun taşıdığı, o masaya bağlı müşteri kimliği |
| `pasif.garson` | `Garson123!` | GARSON | **Pasif hesap**: doğru parolayla bile `401` alır |

Yeni bir masa oluşturulduğunda (`POST /api/tables`) o masa için `masa-{numara}` adında bir müşteri hesabı ve rastgele bir QR anahtarı otomatik üretilir. Anahtar yalnızca bu yanıtta gösterilir. Veritabanında yalnızca Argon2id hash'i saklanır.

## 9. Roller ve yetkiler

Okuma (`*_READ`), ekleme/güncelleme (`*_WRITE`, PUT ve PATCH dahil) ve silme (`*_DELETE`) ayrı yetki düzeyleridir. Rol ve yetki eşlemesi veritabanında tutulur. Yönetici, `POST /api/roles` ile kod değiştirmeden yeni bir rol tanımlayabilir (ör. paket servis platformu için `PAKET_SERVIS`).

| Yetki | MUSTERI | GARSON | KASIYER | MUTFAK | YONETICI |
|---|:-:|:-:|:-:|:-:|:-:|
| MENU_READ (menü) | ✔ | ✔ | ✔ | ✔ | ✔ |
| PRODUCT_READ / WRITE / DELETE (ürün, stok, fiyat) | | | | | ✔ |
| TABLE_READ (masalar) | kendi masası | kendi masaları | ✔ | | ✔ |
| TABLE_WRITE / TABLE_DELETE | | | | | ✔ |
| ORDER_READ (siparişler) | kendi masası | kendi masaları | ✔ | | ✔ |
| ORDER_WRITE (sipariş verme/düzenleme) | yalnızca verme | ✔ | | | |
| ORDER_DELETE (iptal) | | ✔ | | | |
| KITCHEN_READ / KITCHEN_WRITE (mutfak, SSE) | | | | ✔ | |
| BILL_READ (hesap) | kendi masası | kendi masaları | ✔ | | ✔ |
| BILL_WRITE (ödeme, kapatma) | | | ✔ | | |
| USER_* / ROLES_* (kullanıcı ve rol yönetimi) | | | | | ✔ |
| REPORT_READ (raporlar) | | | | | ✔ |
| KNOWLEDGE_* (asistan belgeleri) | | | | | ✔ |
| ASSISTANT_USE (asistan) | | ✔ | ✔ | ✔ | ✔ |
| ACTUATOR_READ (yönetim uç noktaları) | | | | | ✔ |
| ALL_TABLES_ACCESS (sahiplik kısıtı yok) | | | ✔ | | ✔ |

**Kayıt sahipliği:** Müşteri hesabı bir masaya bağlıdır ve yalnızca o masanın **güncel oturumundaki** (açık adisyondaki) sipariş ve hesabını görür. Önceki müşterilerin kapanmış adisyonlarını göremez. Garson yalnızca kendisine atanmış masaların kayıtlarına erişir. Başka bir garsonun masasına erişmeye çalışırsa `403` alır.

## 10. Uç noktalar

| Kaynak | Metotlar | Yol |
|---|---|---|
| Menü | GET | `/api/menu`, `/api/menu/{id}` (`?category=`) |
| Ürünler | GET, POST, PUT, PATCH, DELETE | `/api/products`, `/api/products/{id}` (`?category=&active=`) |
| Masalar | GET, POST, PUT, PATCH, DELETE | `/api/tables`, `/api/tables/{id}`; QR yenileme: `POST /api/tables/{id}/qr-code` |
| Siparişler | GET, POST, PUT, PATCH, DELETE (iptal) | `/api/orders`, `/api/orders/{id}` (`?status=&tableId=`) |
| Mutfak | GET, PATCH | `/api/kitchen/orders`, `/api/kitchen/orders/{id}`, SSE: `GET /api/kitchen/stream` |
| Adisyon ve ödeme | GET, POST, PATCH | `/api/bills`, `/api/bills/{id}`, `/api/bills/{id}/payments` |
| Kullanıcılar | GET, POST, PUT, PATCH, DELETE | `/api/users`, `/api/users/{id}`, `PUT /api/users/{id}/password` |
| Roller ve yetkiler | GET, POST, PUT, PATCH, DELETE | `/api/roles`, `/api/roles/{id}`, `GET /api/permissions` |
| Raporlar | GET | `/api/reports/sales?from=2026-11-01&to=2026-11-06` |
| Asistan | POST | `/api/assistant/questions` |
| Asistan belgeleri | GET, POST, PUT, PATCH, DELETE | `/api/knowledge-documents`, `/api/knowledge-documents/{id}` |

Tüm işlemler ve istek/cevap modelleri Swagger UI'da belgelenmiştir.

**REST kuralları:**
- `POST` her zaman yeni kayıt oluşturur (`201 Created` + `Location` başlığı + üretilen kimlik). Gövdede gönderilen `id` yok sayılır, mevcut kaydı güncelleyemez.
- `PUT` tam güncelleme yapar; gönderilmeyen isteğe bağlı alanlar boşaltılır.
- `PATCH` kısmi güncelleme yapar; yalnızca gönderilen alanlar değişir. Gövdede URL'dekinden farklı bir `id` gönderilirse `400` döner.
- `DELETE` başarılı olduğunda `204 No Content` döner.

## 11. Eşzamanlılık çözümü

Sipariş verme (`OrderServiceImpl.placeOrder`) tek bir veritabanı işleminde (`@Transactional`) şu adımları izler:

1. **Masa satırı kilitlenir** (`PESSIMISTIC_WRITE`). Aynı masaya aynı anda gelen istekler, örneğin çift tıklamalar, sıraya girer.
2. **Idempotency kontrolü yapılır.** Bu `Idempotency-Key` ile oluşturulmuş bir sipariş varsa işlem tekrarlanmaz; mevcut sipariş döner. Stok tekrar düşülmez.
3. **Ürün satırları id sırasıyla kilitlenir** (`SELECT ... WHERE id IN (...) ORDER BY id FOR UPDATE`). Stok yetersizse veya ürün satışta değilse `InsufficientStockException` (409) fırlatılır ve işlemin tamamı geri alınır.
4. **Masanın açık adisyonu kilitlenerek bulunur ya da açılır.** Masa kilidi sayesinde her masada yalnızca bir açık adisyon olabilir.
5. **Sipariş kaydedilir.** `idempotency_key` sütunundaki `UNIQUE` kısıt, çift kaydı veritabanı seviyesinde de engeller.
6. **Mutfağa bildirim gider.** Bildirim yalnızca işlem commit edildikten sonra gönderilir (`@TransactionalEventListener(AFTER_COMMIT)`), bu yüzden geri alınan siparişler mutfağa hiç düşmez.

Ek önlemler:
- Kilit sırası her işlemde aynıdır: **masa → sipariş → ürünler → adisyon**. Bu sıra deadlock'u önler.
- `@Version` alanları (iyimser kilit) sayesinde, örneğin yöneticinin eski veriyle yaptığı bir stok güncellemesi, aynı anda gelen siparişin stok düşümünü ezemez. Bu durumda `409` döner.
- Bağlantılar `READ COMMITTED` yalıtım düzeyinde çalışır. Böylece kilit alındıktan sonraki okumalar her zaman en güncel veriyi görür.
- Ödemelerde de aynı yaklaşım kullanılır: adisyon satır kilidi, `Idempotency-Key` ve `UNIQUE` kısıt.

`ConcurrencyTest`, bu davranışı gerçek iş parçacıklarıyla doğrular:
- 8 masadan aynı anda son porsiyon sipariş edildiğinde yalnızca 1 sipariş onaylanır ve stok 0'da kalır.
- Aynı anahtarla 5 eşzamanlı tıklama yapıldığında tek sipariş oluşur.
- Aynı anahtarla 5 eşzamanlı ödeme isteği gönderildiğinde tutar tek bir kez tahsil edilir.

## 12. Mutfak bildirimi (SSE)

Mutfak ekranı tek bir kalıcı bağlantı açar. Sunucu yalnızca bir şey değiştiğinde olay gönderir:

```bash
curl -N -u mutfak:Mutfak123! http://localhost:8080/api/kitchen/stream
```

Windows PowerShell'de `curl` yerine `curl.exe` yazın. Başka bir terminalden sipariş verdiğinizde akışa şuna benzer bir olay düşer:

```
event:order-created
data:{"orderId":15,"tableNumber":1,"status":"CONFIRMED","statusLabel":"Onaylandı","items":[{"productName":"Ayran","quantity":2,"note":"Soğuk olsun"}], ...}
```

Olay türleri şunlardır: `connected`, `order-created`, `order-updated`, `order-status-changed`, `order-cancelled`. Bağlantıyı canlı tutmak için her 25 saniyede bir heartbeat yorumu gönderilir.

`SseKitchenNotifier`, bean yaşam döngüsünü de gösterir:
- `@PostConstruct`: heartbeat zamanlayıcısını başlatır.
- `@PreDestroy`: zamanlayıcıyı durdurur ve açık bağlantıları düzgünce kapatır.

## 13. Personel asistanı (RAG)

- **Bilgi kaynağı:** Yalnızca `knowledge_documents` tablosundaki menü ve alerjen belgeleri kullanılır. Bu belgeler ilk açılışta `src/main/resources/knowledge/` klasöründen yüklenir. Yönetici `/api/knowledge-documents` ile yeni belge yükleyebilir; asistan indeksi otomatik olarak yenilenir.
- **Akış (Spring AI):**
  1. Belgeler parçalara bölünür ve `gemini-embedding-001` ile vektöre çevrilir (`SimpleVectorStore`).
  2. Soruya en benzer parçalar bulunur (top-k = 4, benzerlik eşiği = 0.55).
  3. Eşiği geçen parça **yoksa model çağrılmaz** ve "belgelerde dayanak bulunamadı" cevabı döner.
  4. Varsa model yalnızca bu parçalarla, sıcaklık 0'da ve sıkı bir sistem talimatıyla cevap verir.
- **Sınırlar:** Belgede açıkça yazmayan hiçbir bilgi cevaba eklenmez. Soru içine gizlenmiş "kuralları unut" türü talimatlar yok sayılır. Konu dışı sorular yanıtlanmaz. Cevapta `grounded` alanı, kaynak belgeler (`sources`) ve bir alerjen uyarısı bulunur.
- **İki gerçekleştirim:** `MenuAssistant` arayüzünün iki gerçekleştirimi vardır:
  - `RagMenuAssistant`: `@Primary` ve `@Lazy`. İlk soru sorulana kadar oluşturulmaz.
  - `KeywordMenuAssistant`: `@Qualifier("keywordMenuAssistant")` ile seçilir. Yapay zeka kullanmaz, belgelerden birebir alıntı yapar.

  Gemini anahtarı tanımlı değilse ya da Gemini'ye ulaşılamazsa sistem otomatik olarak güvenli moda geçer.

```bash
curl -u garson1:Garson123! -H "Content-Type: application/json" \
     -d "{\"question\":\"Fıstıklı baklavada hangi alerjenler var?\"}" \
     http://localhost:8080/api/assistant/questions
```

## 14. Hata modeli

Tüm hatalar `GlobalExceptionHandler` (`@RestControllerAdvice`) içinde tek merkezden yönetilir ve aynı JSON modeliyle döner. Güvenlik filtresindeki 401/403 hataları da bu sınıfa yönlendirilir.

```json
{
  "timestamp": "2026-11-06T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ürün bulunamadı: id=999",
  "path": "/api/products/999"
}
```

| Kod | Ne zaman |
|---|---|
| 400 | Doğrulama hatası, bozuk JSON, hatalı parametre, eksik `Idempotency-Key`, PATCH ile id değiştirme |
| 401 | Kimlik bilgisi yok, kullanıcı adı/parola hatalı veya hesap pasif (`WWW-Authenticate: Basic`) |
| 403 | Gerekli yetki yok veya kayıt sahipliği kuralı ihlal edildi |
| 404 | Kayıt veya adres bulunamadı |
| 409 | Kaydın mevcut durumuyla çakışma: yetersiz stok, kapalı adisyon, geçersiz durum geçişi, tekrar eden benzersiz değer, eşzamanlı güncelleme |

Uygulamaya özgü hata sınıfları `exception` paketindedir: `ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `InsufficientStockException`, `ForbiddenOperationException`.

## 15. Testler

```bash
./mvnw test
```

Testler Docker gerektirmez; MySQL uyumluluk kipindeki bellek içi H2 veritabanını kullanır (yalnızca test profili). Uygulama ise her zaman MySQL ile çalışır.

| Test sınıfı | Kapsam |
|---|---|
| `ProductApiTest` | CRUD, PUT/PATCH farkı, POST'ta id yok sayma, PATCH ile id değiştirme (400), 400/401/403/404 |
| `OrderFlowTest` | QR müşteri siparişi → idempotent tekrar → kayıt sahipliği (403) → mutfak durumları → ödeme → adisyon kapatma |
| `ConcurrencyTest` | Son porsiyon yarışı, çift tıklama ile sipariş ve çifte ödeme (gerçek eşzamanlı iş parçacıkları) |
| `KitchenStreamTest` | Yeni siparişin mutfağın SSE bağlantısına anında düşmesi, olayda fiyat bilgisi olmaması, akışın yalnızca MUTFAK rolüne açık olması |
| `SecurityAndPlatformTest` | Argon2id hash'i, Actuator erişim kuralları, OpenAPI/Swagger, masa QR hesabı, yeni rol tanımlama |
| `AssistantTest` | `@Lazy` RAG bean'i, belgeye dayalı cevap, belge dışı soruda uydurmama, yeni belgenin kullanılması |

## 16. Postman koleksiyonu

1. Postman'de **Import** düğmesine basıp `postman/Restoran-POS-API.postman_collection.json` dosyasını seçin.
2. Gerekirse **Collection → Variables** bölümünden `baseUrl` ve kullanıcı bilgilerini değiştirin. Tüm kimlik bilgileri değişkenlerle yönetilir.
3. **Run collection** ile tüm istekleri sırayla çalıştırın. Her isteğin durum kodu testleri vardır.

| Klasör | İçerik |
|---|---|
| 1 - Ürün CRUD | Listeleme, filtreleme, tek kayıt, ekleme, PUT, PATCH, DELETE |
| 2 - Hata Senaryoları | 400 (doğrulama, id değiştirme, bozuk JSON), 404, 401 (yok, hatalı, pasif), 403 (yetki, sahiplik) |
| 3 - Sipariş Akışı | QR sipariş, çift tıklama (idempotency), sahiplik 403'leri, mutfak, ödeme, kapatma, 409 stok |
| 4 - Personel Asistanı | Alerjen sorusu, belge dışı soru, belge yükleme |
| 5 - Kullanıcı ve Rol Yönetimi | Argon2id ile kullanıcı oluşturma, pasifleştirme, silme |
| 6 - Actuator ve Dokümantasyon | health, info, metrics (yönetici / 403), OpenAPI |

## 17. Proje yapısı

```
src/main/java/com/restoran/pos
├── config/        SecurityConfig, OpenApiConfig, AppConfig      (@Configuration + @Bean)
├── controller/    @RestController sınıfları                     (HTTP istek/cevap)
├── service/       Servis arayüzleri
│   └── impl/      @Service gerçekleştirimleri                   (iş kuralları, @Transactional)
├── repository/    Repository arayüzleri
│   └── impl/      @Repository sınıfları                         (EntityManager + JPQL)
├── entity/        @Entity sınıfları ve enum'lar
├── dto/           İstek/cevap modelleri (record)
├── security/      Kullanıcı yükleme, yetki sabitleri, kayıt sahipliği, 401/403 yönlendirme
├── exception/     Özel hata sınıfları, ApiError, GlobalExceptionHandler
├── notification/  SSE mutfak bildirimi
├── assistant/     RAG ve anahtar kelime asistanları
├── bootstrap/     Örnek veri yükleyici
└── util/          Yardımcılar (PATCH koruması, idempotency anahtarı, metin)
```

İstek akışı her zaman **Controller → Service → Repository → Veritabanı** şeklindedir.

## 18. Teknik gereklilikler kontrol listesi

| Gereklilik | Karşılandığı yer |
|---|---|
| Spring Boot 4.1.2 | `pom.xml` (parent 4.1.2) |
| Spring Data JPA/REST kullanılmaz | `pom.xml`: yalnızca `spring-boot-starter-jdbc` + `spring-boot-hibernate`; hiçbir `JpaRepository`/`CrudRepository` yok |
| Controller → Service → Repository → DB | `controller/`, `service/` + `service/impl/`, `repository/` + `repository/impl/` |
| `@RestController`, controller'da DB işlemi yok | Tüm controller'lar yalnızca servis çağırır |
| Servis arayüzleri + `@Service` sınıfları | `service/*Service` + `service/impl/*ServiceImpl` |
| Repository arayüzleri + `@Repository` sınıfları, `EntityManager` | `repository/impl/*RepositoryImpl` (`persist`, `find`, `createQuery`, `merge`, `remove`) |
| Yalnızca constructor injection | Tüm bean'ler (`EntityManager` dahil) constructor ile alınır; `@Autowired` alan/setter yok |
| `@Configuration` + `@Bean` | `AppConfig` (`Clock`, `SecureRandom`), `SecurityConfig` (`SecurityFilterChain`, `PasswordEncoder`), `OpenApiConfig` (`OpenAPI`, `OpenApiCustomizer`) |
| `@Lazy` | `RagMenuAssistant` (bean) ve `AssistantServiceImpl` constructor parametresi |
| `@PostConstruct` / `@PreDestroy` | `SseKitchenNotifier` (heartbeat başlatma / bağlantıları kapatma), `RagMenuAssistant` |
| Aynı arayüzün birden fazla gerçekleştirimi + `@Qualifier`/`@Primary` | `MenuAssistant`: `RagMenuAssistant` (`@Primary`), `KeywordMenuAssistant` (`@Qualifier`) |
| Özel property + `@Value` ile constructor | `pos.*` ayarları (bkz. bölüm 6) |
| Actuator health + info (ad, açıklama, sürüm) | `application.yml` → `info.app.*`, `management.*` |
| Yönetim endpoint'leri korunur | `/actuator/metrics` yalnızca `ACTUATOR_READ` |
| MySQL'de kalıcı veri | `docker-compose.yml` (`mysql-data` birimi), `ddl-auto=update` |
| `@Entity`, `@Table`, `@Column`; parametresiz constructor; `@Id` + `@GeneratedValue(IDENTITY)` | `entity/` paketindeki tüm sınıflar |
| Ekleme, bulma, listeleme, güncelleme, silme | Her `*RepositoryImpl` |
| Parametreli JPQL + `setParameter` | ör. `ProductRepositoryImpl.findByFilter`, `UserAccountRepositoryImpl.search`, `OrderRepositoryImpl.search` |
| `@Transactional` servis katmanında | `service/impl/*` (`org.springframework.transaction.annotation.Transactional`) |
| Kaynak odaklı URL'ler, uygun HTTP metotları | Bölüm 10 |
| GET liste + `@PathVariable` ile tek kayıt | Tüm kaynaklar |
| POST + üretilen kimlik, istemci id'si güncelleme yapamaz | `ProductController.create`, `ProductServiceImpl.create` (her zaman `persist`) |
| PUT tam, PATCH kısmi güncelleme, PATCH ile PK değişmez | `*UpdateRequest`, `*PatchRequest`, `PatchGuard` |
| DELETE | Tüm kaynaklar (siparişte iptal) |
| `@RequestBody` + Jackson | Tüm controller'lar |
| Global exception handler + özel hata modeli | `GlobalExceptionHandler`, `ApiError`, özel hata sınıfları |
| 404 / 400 tutarlı | Bölüm 14 |
| `SecurityFilterChain` + HTTP Basic | `SecurityConfig` |
| Kullanıcı, aktiflik ve roller DB'de | `users`, `roles`, `permissions`, `user_roles`, `role_permissions` tabloları; `DatabaseUserDetailsService` |
| Argon2id | `SecurityConfig.passwordEncoder()`; `password_hash` 255 karakter |
| Okuma / ekleme-güncelleme / silme ayrı yetki, PATCH = güncelleme | `SecurityConfig` (metot + yol eşleşmeleri) |
| 401 / 403 | `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` → `GlobalExceptionHandler` |
| OpenAPI + Swagger UI | springdoc; `/swagger-ui.html`, `/v3/api-docs` |
| Çalıştırma rehberi | Bu belge |
| Postman koleksiyonu (değişkenlerle) | `postman/Restoran-POS-API.postman_collection.json` |
