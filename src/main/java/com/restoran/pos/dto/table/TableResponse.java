package com.restoran.pos.dto.table;

import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Masa")
public record TableResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Integer tableNumber,
        @Schema(example = "4") Integer capacity,
        @Schema(example = "Salon") String location,
        boolean active,
        @Schema(description = "Masada açık adisyon varsa true") boolean occupied,
        @Schema(example = "2") Long waiterId,
        @Schema(example = "Ahmet Garson") String waiterName,
        Instant createdAt,
        Instant updatedAt) {

    public static TableResponse from(RestaurantTable table, boolean occupied) {
        UserAccount waiter = table.getWaiter();
        return new TableResponse(table.getId(), table.getTableNumber(), table.getCapacity(), table.getLocation(),
                table.isActive(), occupied,
                waiter != null ? waiter.getId() : null,
                waiter != null ? waiter.getFullName() : null,
                table.getCreatedAt(), table.getUpdatedAt());
    }
}
