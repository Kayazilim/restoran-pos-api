package com.restoran.pos.dto.report;

import java.math.BigDecimal;

public record TopProductResponse(String productName, long quantity, BigDecimal revenue) {
}
