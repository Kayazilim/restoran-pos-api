package com.restoran.pos.dto.table;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Oluşturulan masa (üretilen kimlikle) ve masanın QR müşteri hesabı bilgileri")
public record TableCreatedResponse(TableResponse table, QrCredentialsResponse qrCredentials) {
}
