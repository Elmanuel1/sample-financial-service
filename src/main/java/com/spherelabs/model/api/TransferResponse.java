package com.spherelabs.model.api;

import com.spherelabs.model.Transaction;

import java.time.OffsetDateTime;

public record TransferResponse(String internalID, Transaction.Status status, OffsetDateTime createdAt, OffsetDateTime updatedAt, String message) { }
