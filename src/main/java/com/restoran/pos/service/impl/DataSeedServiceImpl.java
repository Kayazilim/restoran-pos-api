package com.restoran.pos.service.impl;

import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.entity.KnowledgeDocument;
import com.restoran.pos.entity.Permission;
import com.restoran.pos.entity.Product;
import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.Role;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.repository.KnowledgeDocumentRepository;
import com.restoran.pos.repository.PermissionRepository;
import com.restoran.pos.repository.ProductRepository;
import com.restoran.pos.repository.RestaurantTableRepository;
import com.restoran.pos.repository.RoleRepository;
import com.restoran.pos.repository.UserAccountRepository;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.SystemRoles;
import com.restoran.pos.service.DataSeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * İlk açılışta örnek verileri yükler. Veriler MySQL'de kalıcı olduğundan sonraki açılışlarda yalnızca
 * yetkiler ve sistem rolleri koddaki tanımla eşitlenir; mevcut kayıtlar korunur.
 */
@Service
public class DataSeedServiceImpl implements DataSeedService {

    private static final Logger log = LoggerFactory.getLogger(DataSeedServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final RestaurantTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeedServiceImpl(PermissionRepository permissionRepository,
                               RoleRepository roleRepository,
                               UserAccountRepository userAccountRepository,
                               RestaurantTableRepository tableRepository,
                               ProductRepository productRepository,
                               KnowledgeDocumentRepository documentRepository,
                               PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void seed() {
        Map<String, Permission> permissions = syncPermissions();
        Map<String, Role> roles = syncSystemRoles(permissions);
        if (userAccountRepository.count() == 0) {
            seedUsersAndTables(roles);
            log.info("Örnek kullanıcılar ve masalar yüklendi.");
        }
        if (productRepository.findAll().isEmpty()) {
            seedProducts();
            log.info("Örnek menü ürünleri yüklendi.");
        }
        if (documentRepository.count() == 0) {
            seedKnowledgeDocuments();
            log.info("Asistan için menü ve alerjen belgeleri yüklendi.");
        }
    }

    private Map<String, Permission> syncPermissions() {
        Map<String, Permission> permissions = new HashMap<>();
        Permissions.DESCRIPTIONS.forEach((name, description) -> permissions.put(name,
                permissionRepository.findByName(name)
                        .orElseGet(() -> permissionRepository.save(new Permission(name, description)))));
        return permissions;
    }

    private Map<String, Role> syncSystemRoles(Map<String, Permission> permissions) {
        Map<String, Role> roles = new HashMap<>();
        for (SystemRoles.Definition definition : SystemRoles.DEFINITIONS) {
            List<Permission> rolePermissions = definition.permissions().stream().map(permissions::get).toList();
            Role role = roleRepository.findByName(definition.name()).orElse(null);
            if (role == null) {
                role = new Role(definition.name(), definition.description(), true);
                role.replacePermissions(rolePermissions);
                roleRepository.save(role);
            } else {
                role.setDescription(definition.description());
                role.replacePermissions(rolePermissions);
                roleRepository.update(role);
            }
            roles.put(role.getName(), role);
        }
        return roles;
    }

    private void seedUsersAndTables(Map<String, Role> roles) {
        createUser("yonetici", "Yonetici123!", "Ayşe Yönetici", true, roles.get(SystemRoles.YONETICI), null);
        UserAccount waiter1 = createUser("garson1", "Garson123!", "Ahmet Garson", true, roles.get(SystemRoles.GARSON), null);
        UserAccount waiter2 = createUser("garson2", "Garson123!", "Elif Garson", true, roles.get(SystemRoles.GARSON), null);
        createUser("kasiyer", "Kasiyer123!", "Mehmet Kasiyer", true, roles.get(SystemRoles.KASIYER), null);
        createUser("mutfak", "Mutfak123!", "Fatma Mutfak", true, roles.get(SystemRoles.MUTFAK), null);
        // Aktiflik bilgisini göstermek için pasif bir hesap: doğru parolayla bile giriş yapamaz (401).
        createUser("pasif.garson", "Garson123!", "Pasif Garson", false, roles.get(SystemRoles.GARSON), null);

        for (int number = 1; number <= 6; number++) {
            UserAccount waiter = number <= 3 ? waiter1 : waiter2;
            String location = number <= 4 ? "Salon" : "Bahçe";
            RestaurantTable table = tableRepository.save(
                    new RestaurantTable(number, number % 3 == 0 ? 6 : 4, location, waiter, true));
            createUser("masa-" + number, "qr-masa-" + number, "Masa " + number + " müşterisi", true,
                    roles.get(SystemRoles.MUSTERI), table);
        }
    }

    private UserAccount createUser(String username, String password, String fullName, boolean enabled,
                                   Role role, RestaurantTable table) {
        UserAccount user = new UserAccount(username, passwordEncoder.encode(password), fullName, enabled);
        user.replaceRoles(List.of(role));
        user.setTable(table);
        return userAccountRepository.save(user);
    }

    private void seedProducts() {
        List.of(
                product("Mercimek Çorbası", "Kırmızı mercimek, soğan ve havuçla; tereyağlı pul biber sosuyla",
                        "Çorbalar", "120.00", 50, "süt,kereviz"),
                product("Ezogelin Çorbası", "Kırmızı mercimek, bulgur ve pirinçle; nane ve tereyağı ile",
                        "Çorbalar", "120.00", 40, "gluten,süt"),
                product("Izgara Köfte", "Dana kıyma, soğan, ekmek içi ve yumurta; pilav ve közlenmiş biber ile",
                        "Ana Yemekler", "380.00", 30, "gluten,yumurta,süt"),
                product("Tavuk Şiş", "Yoğurtlu sosta marine edilmiş tavuk göğsü; bulgur pilavı ile",
                        "Ana Yemekler", "340.00", 30, "süt,gluten"),
                product("Sebzeli Güveç", "Patlıcan, kabak, biber ve domatesle zeytinyağlı güveç (vegan)",
                        "Ana Yemekler", "300.00", 15, ""),
                product("Çoban Salata", "Domates, salatalık, biber, soğan, maydanoz; zeytinyağı ve limon",
                        "Salatalar", "140.00", 25, ""),
                product("Fırın Sütlaç", "Süt, pirinç ve şekerle fırında pişirilir", "Tatlılar", "150.00", 20, "süt"),
                product("Fıstıklı Baklava", "Tereyağlı yufka ve bol Antep fıstığı (son 1 porsiyon)",
                        "Tatlılar", "220.00", 1, "gluten,süt,antep fıstığı"),
                product("Künefe", "Tel kadayıf ve hatay peyniri; şerbetli (şu an tükendi)",
                        "Tatlılar", "260.00", 0, "gluten,süt,antep fıstığı"),
                product("Ayran", "Yoğurt, su ve tuz", "İçecekler", "50.00", 100, "süt"),
                product("Türk Kahvesi", "Orta şekerli, lokum ile", "İçecekler", "80.00", 100, "fındık"),
                product("Limonata", "Taze sıkılmış limon ve nane", "İçecekler", "90.00", 60, "")
        ).forEach(productRepository::save);
    }

    private static Product product(String name, String description, String category, String price,
                                   int stock, String allergens) {
        return new Product(name, description, category, new BigDecimal(price), stock,
                allergens.isBlank() ? null : allergens, true);
    }

    private void seedKnowledgeDocuments() {
        documentRepository.save(new KnowledgeDocument("Menü Bilgileri", DocumentType.MENU,
                readClasspathText("knowledge/menu-bilgileri.md")));
        documentRepository.save(new KnowledgeDocument("Alerjen Bilgileri", DocumentType.ALLERGEN,
                readClasspathText("knowledge/alerjen-bilgileri.md")));
    }

    private static String readClasspathText(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8).strip();
        } catch (IOException ex) {
            throw new UncheckedIOException("Belge okunamadı: " + path, ex);
        }
    }
}
