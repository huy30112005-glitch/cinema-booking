package com.cinema.service;

import com.cinema.dto.payment.BankTransferInfoRequest;
import com.cinema.dto.payment.BankTransferInfoResponse;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class BankTransferInfoService {

    private final JdbcTemplate jdbcTemplate;

    public BankTransferInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BankTransferInfoResponse getInfo() {
        ensureTable();
        BankTransferInfo data = readInfo();
        return toResponse(data);
    }

    @Transactional
    public BankTransferInfoResponse updateInfo(BankTransferInfoRequest request) {
        String bankCode = normalize(request.bankCode());
        String bankName = normalize(request.bankName());
        String accountName = normalize(request.accountName());
        String accountNumber = normalize(request.accountNumber());

        if (bankCode.isBlank() || bankName.isBlank() || accountName.isBlank() || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn ngân hàng và nhập đủ thông tin tài khoản");
        }

        BankTransferInfo data = new BankTransferInfo(bankCode, bankName, accountName, accountNumber);
        ensureTable();
        writeInfo(data);
        return toResponse(data);
    }

    private void ensureTable() {
        jdbcTemplate.execute("""
                create table if not exists bank_transfer_info (
                    id integer primary key,
                    bank_code varchar(32) not null,
                    bank_name varchar(120) not null,
                    account_name varchar(160) not null,
                    account_number varchar(64) not null
                )
                """);
    }

    private BankTransferInfo readInfo() {
        try {
            return jdbcTemplate.queryForObject("""
                    select bank_code, bank_name, account_name, account_number
                    from bank_transfer_info
                    where id = 1
                    """, (rs, rowNum) -> mapInfo(rs));
        } catch (EmptyResultDataAccessException e) {
            return new BankTransferInfo("", "", "", "");
        }
    }

    private void writeInfo(BankTransferInfo data) {
        jdbcTemplate.update("""
                insert into bank_transfer_info (id, bank_code, bank_name, account_name, account_number)
                values (1, ?, ?, ?, ?)
                on conflict (id) do update set
                    bank_code = excluded.bank_code,
                    bank_name = excluded.bank_name,
                    account_name = excluded.account_name,
                    account_number = excluded.account_number
                """,
                data.bankCode(),
                data.bankName(),
                data.accountName(),
                data.accountNumber());
    }

    private BankTransferInfo mapInfo(ResultSet rs) throws SQLException {
        return new BankTransferInfo(
                rs.getString("bank_code"),
                rs.getString("bank_name"),
                rs.getString("account_name"),
                rs.getString("account_number")
        );
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
