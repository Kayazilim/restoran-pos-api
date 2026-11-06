package com.restoran.pos.controller;

import com.restoran.pos.dto.product.MenuItemResponse;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menü", description = "Tüm rollerin gördüğü menü; stok adedi içermez (MENU_READ)")
public class MenuController {

    private final ProductService productService;

    public MenuController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.MENU_READ + "')")
    @Operation(summary = "Menüyü listele", description = "Aktif ürünleri listeler; isteğe bağlı kategori filtresi uygulanır.")
    public List<MenuItemResponse> findMenu(
            @Parameter(description = "Kategori filtresi", example = "Tatlılar")
            @RequestParam(required = false) String category) {
        return productService.findMenu(category);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.MENU_READ + "')")
    @Operation(summary = "Menü kalemini getir")
    public MenuItemResponse findMenuItem(@PathVariable Long id) {
        return productService.findMenuItem(id);
    }
}
