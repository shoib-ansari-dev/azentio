package com.customer.support.ai.appserver.dto;

import java.util.UUID;

public record BulkUploadResponse(UUID jobId, String status, String message) {
}
