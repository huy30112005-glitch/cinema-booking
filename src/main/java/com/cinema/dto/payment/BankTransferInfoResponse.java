package com.cinema.dto.payment;

public record BankTransferInfoResponse(
        String bankCode,
        String bankName,
        String accountName,
        String accountNumber,
        boolean configured
) {
}
