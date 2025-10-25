// src/main/java/com/fonestore/user_api/dto/ShipmentDTO.java
package com.fonestore.user_api.dto;

import java.math.BigDecimal;


public record ShipmentDTO(
  Long shipId,
  String carrier,
  String trackingNo,
  String status,
  BigDecimal fee,
  java.time.LocalDateTime createdAt,
  java.time.LocalDateTime updatedAt
) {}

