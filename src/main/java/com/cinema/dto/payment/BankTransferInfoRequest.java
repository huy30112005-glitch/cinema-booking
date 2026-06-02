package com.cinema.dto.payment;

public record BankTransferInfoRequest(
        String bankCode,
        String bankName,
        String accountName,
        String accountNumber
) {
}
