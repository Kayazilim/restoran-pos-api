package com.restoran.pos.controller;

import com.restoran.pos.dto.user.PasswordChangeRequest;
import com.restoran.pos.dto.user.UserCreateRequest;
import com.restoran.pos.dto.user.UserPatchRequest;
import com.restoran.pos.dto.user.UserResponse;
import com.restoran.pos.dto.user.UserUpdateRequest;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Kullanıcılar", description = "Hesaplar ve rol atamaları (yalnızca YÖNETİCİ)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    @Operation(summary = "Kullanıcıları listele", description = "Rol ve aktiflik bilgisine göre parametreli JPQL ile filtrelenebilir.")
    public List<UserResponse> findAll(
            @Parameter(description = "Rol filtresi", example = "GARSON") @RequestParam(required = false) String role,
            @Parameter(description = "Aktiflik filtresi", example = "true") @RequestParam(required = false) Boolean enabled) {
        return userService.findAll(role, enabled);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    @Operation(summary = "Kimliğe göre kullanıcı getir")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_WRITE + "')")
    @Operation(summary = "Yeni kullanıcı oluştur", description = "Parola Argon2id ile hash'lenerek saklanır.")
    @ApiResponse(responseCode = "201", description = "Kullanıcı oluşturuldu",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_WRITE + "')")
    @Operation(summary = "Kullanıcıyı tam güncelle (PUT)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                               @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return userService.update(id, request, principal.currentUser());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_WRITE + "')")
    @Operation(summary = "Kullanıcıyı kısmi güncelle (PATCH)", description = "Ör. hesabı pasifleştirme veya rol atama.")
    public UserResponse patch(@PathVariable Long id, @Valid @RequestBody UserPatchRequest request,
                              @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return userService.patch(id, request, principal.currentUser());
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('" + Permissions.USER_WRITE + "')")
    @Operation(summary = "Kullanıcının parolasını değiştir")
    @ApiResponse(responseCode = "204", description = "Parola değiştirildi")
    public ResponseEntity<Void> changePassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_DELETE + "')")
    @Operation(summary = "Kullanıcıyı sil", description = "İşlem geçmişi olan kullanıcı silinemez (409); pasifleştirilebilir.")
    @ApiResponse(responseCode = "204", description = "Kullanıcı silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        userService.delete(id, principal.currentUser());
        return ResponseEntity.noContent().build();
    }
}
