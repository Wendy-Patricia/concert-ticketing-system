package com.example.concertticketingsystem.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erreur HTTP structurée, sans trace interne ni SQL", requiredProperties = {"timestamp", "status", "code", "message", "path"})
public record ApiError(Instant timestamp, int status, String code, String message, String path) {
}
