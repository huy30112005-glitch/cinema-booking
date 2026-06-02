package com.cinema.controller;

import com.cinema.dto.payment.ConfirmPaymentRequest;
import com.cinema.dto.payment.BankTransferInfoRequest;
import com.cinema.dto.payment.CreatePaymentRequest;
import com.cinema.dto.payment.MomoIpnResponse;
import com.cinema.entity.NguoiDung;
import com.cinema.service.BankTransferInfoService;
import com.cinema.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BankTransferInfoService bankTransferInfoService;

    public PaymentController(
            PaymentService paymentService,
            BankTransferInfoService bankTransferInfoService) {
        this.paymentService = paymentService;
        this.bankTransferInfoService = bankTransferInfoService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestBody CreatePaymentRequest request,
            HttpServletRequest servletRequest,
            HttpSession session) {

        return paymentService.createPayment(request, servletRequest, session);
    }

    @GetMapping("/{maThanhToan}")
    public ResponseEntity<?> detail(
            @PathVariable Integer maThanhToan,
            HttpSession session) {

        return paymentService.getPaymentDetail(maThanhToan, session);
    }

    @GetMapping("/bank-info")
    public ResponseEntity<?> bankInfo() {
        return ResponseEntity.ok(bankTransferInfoService.getInfo());
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<?> myTickets(HttpSession session) {
        return paymentService.getMyTickets(session);
    }

    @PutMapping("/bank-info")
    public ResponseEntity<?> updateBankInfo(
            @RequestBody BankTransferInfoRequest request,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được cập nhật thông tin chuyển khoản");
        }

        try {
            return ResponseEntity.ok(bankTransferInfoService.updateInfo(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/success")
    public ResponseEntity<?> success(
            @RequestBody ConfirmPaymentRequest request,
            HttpSession session) {

        return paymentService.confirmSuccess(request, session);
    }

    @GetMapping("/admin/pending-bank")
    public ResponseEntity<?> pendingBankPayments(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được xem thanh toán chờ duyệt");
        }

        return paymentService.getPendingBankPayments();
    }

    @PostMapping("/admin/{maThanhToan}/approve")
    public ResponseEntity<?> approveBankPayment(
            @PathVariable Integer maThanhToan,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được xác nhận thanh toán");
        }

        return paymentService.approveBankPayment(maThanhToan);
    }

    @PostMapping("/admin/{maThanhToan}/reject")
    public ResponseEntity<?> rejectBankPayment(
            @PathVariable Integer maThanhToan,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Chỉ admin được từ chối thanh toán");
        }

        return paymentService.rejectBankPayment(maThanhToan);
    }

    @GetMapping("/momo-return")
    public ResponseEntity<String> momoReturn(@RequestParam Map<String, String> params) {
        return paymentService.handleMomoReturn(params);
    }

    @PostMapping("/momo-ipn")
    public ResponseEntity<MomoIpnResponse> momoIpn(@RequestBody Map<String, Object> body) {
        return paymentService.handleMomoIpn(body);
    }

    private boolean isAdmin(HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");
        String vaiTro = user != null && user.getVaiTro() != null
                ? user.getVaiTro().getTenVaiTro()
                : "";

        return "ADMIN".equalsIgnoreCase(vaiTro);
    }
}
