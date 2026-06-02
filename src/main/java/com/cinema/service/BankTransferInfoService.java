package com.cinema.service;

import com.cinema.dto.payment.BankTransferInfoRequest;
import com.cinema.dto.payment.BankTransferInfoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class BankTransferInfoService {

    private final ObjectMapper objectMapper;
    private final Path storagePath;

    public BankTransferInfoService(
            ObjectMapper objectMapper,
            @Value("${payment.bank-info-file:bank-transfer-info.json}") String bankInfoFile) {
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(bankInfoFile);
    }

    public BankTransferInfoResponse getInfo() {
        BankTransferInfo data = readInfo();
        return toResponse(data);
    }

    public BankTransferInfoResponse updateInfo(BankTransferInfoRequest request) {
        String bankCode = normalize(request.bankCode());
        String bankName = normalize(request.bankName());
        String accountName = normalize(request.accountName());
        String accountNumber = normalize(request.accountNumber());

        if (bankCode.isBlank() || bankName.isBlank() || accountName.isBlank() || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn ngân hàng và nhập đủ thông tin tài khoản");
        }

        BankTransferInfo data = new BankTransferInfo(bankCode, bankName, accountName, accountNumber);
        writeInfo(data);
        return toResponse(data);
    }

    private BankTransferInfo readInfo() {
        if (!Files.exists(storagePath)) {
            return new BankTransferInfo("", "", "", "");
        }

        try {
            return objectMapper.readValue(storagePath.toFile(), BankTransferInfo.class);
        } catch (IOException e) {
            return new BankTransferInfo("", "", "", "");
        }
    }

    private void writeInfo(BankTransferInfo data) {
        try {
            Path parent = storagePath.toAbsolutePath().getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), data);
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được thông tin chuyển khoản");
        }
    }

    private BankTransferInfoResponse toResponse(BankTransferInfo data) {
        String bankCode = normalize(data.bankCode());
        String bankName = normalize(data.bankName());
        String accountName = normalize(data.accountName());
        String accountNumber = normalize(data.accountNumber());

        return new BankTransferInfoResponse(
                bankCode,
                bankName,
                accountName,
                accountNumber,
                !bankCode.isBlank() && !accountName.isBlank() && !accountNumber.isBlank()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record BankTransferInfo(
            String bankCode,
            String bankName,
            String accountName,
            String accountNumber
    ) {
    }
}
