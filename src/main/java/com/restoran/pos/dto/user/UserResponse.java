package com.restoran.pos.dto.user;

import com.restoran.pos.entity.Role;
import com.restoran.pos.entity.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Kullanıcı hesabı (parola hash'i hiçbir zaman döndürülmez)")
public record UserResponse(
        @Schema(example = "2") Long id,
        @Schema(example = "garson1") String username,
        @Schema(example = "Ahmet Yılmaz") String fullName,
        boolean enabled,
        List<String> roles,
        @Schema(description = "Masaya bağlı müşteri hesaplarında masa kimliği") Long tableId,
        Integer tableNumber,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(UserAccount user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.isEnabled(), roles,
                user.getTable() != null ? user.getTable().getId() : null,
                user.getTable() != null ? user.getTable().getTableNumber() : null,
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
