package com.cinema.service;

import com.cinema.config.MomoProperties;
import com.cinema.dto.VeDTO;
import com.cinema.dto.payment.ConfirmPaymentRequest;
import com.cinema.dto.payment.CreatePaymentRequest;
import com.cinema.dto.payment.CreatePaymentResponse;
import com.cinema.dto.payment.MomoIpnResponse;
import com.cinema.dto.payment.PaymentDetailResponse;
import com.cinema.entity.*;
import com.cinema.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final String PAYMENT_SESSION_PREFIX = "payment:";
    private static final String PHUONG_THUC_GIA_LAP = "Thanh toán giả lập";
    private static final String PHUONG_THUC_MOMO = "MoMo";
    private static final String TRANG_THAI_CHO_KHACH = "CREATED";
    private static final String TRANG_THAI_CHO_ADMIN = "WAITING_ADMIN";
    private static final String TRANG_THAI_DA_DUYET = "CONFIRMED";
    private static final String TRANG_THAI_TU_CHOI = "REJECTED";
    private static final Duration ADMIN_REVIEW_HOLD_DURATION = Duration.ofHours(24);

    private final SuatChieuRepository suatChieuRepository;
    private final GheRepository gheRepository;
    private final VeRepository veRepository;
    private final DonHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final PhuongThucThanhToanRepository phuongThucRepository;
    private final VeService veService;
    private final SeatHoldService seatHoldService;
    private final MomoProperties momoProperties;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public PaymentService(
            SuatChieuRepository suatChieuRepository,
            GheRepository gheRepository,
            VeRepository veRepository,
            DonHangRepository donHangRepository,
            ThanhToanRepository thanhToanRepository,
            PhuongThucThanhToanRepository phuongThucRepository,
            VeService veService,
            SeatHoldService seatHoldService,
            MomoProperties momoProperties,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate) {
        this.suatChieuRepository = suatChieuRepository;
        this.gheRepository = gheRepository;
        this.veRepository = veRepository;
        this.donHangRepository = donHangRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.phuongThucRepository = phuongThucRepository;
        this.veService = veService;
        this.seatHoldService = seatHoldService;
        this.momoProperties = momoProperties;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void initPendingPaymentSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS thanh_toan_cho_duyet (
                    ma_thanh_toan INTEGER PRIMARY KEY,
                    ma_suat_chieu INTEGER NOT NULL,
                    ma_nguoi_dung INTEGER NOT NULL,
                    trang_thai VARCHAR(32) NOT NULL,
                    thoi_gian_tao TIMESTAMP NOT NULL,
                    thoi_gian_yeu_cau TIMESTAMP NULL,
                    thoi_gian_xu_ly TIMESTAMP NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS thanh_toan_cho_duyet_ghe (
                    ma_thanh_toan INTEGER NOT NULL,
                    ma_ghe INTEGER NOT NULL,
                    PRIMARY KEY (ma_thanh_toan, ma_ghe)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS thanh_toan_cho_duyet_ghe_chi_tiet (
                    ma_thanh_toan INTEGER NOT NULL,
                    ma_suat_chieu INTEGER NOT NULL,
                    ma_ghe INTEGER NOT NULL,
                    PRIMARY KEY (ma_thanh_toan, ma_suat_chieu, ma_ghe)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS thanh_toan_ve (
                    ma_thanh_toan INTEGER NOT NULL,
                    ma_ve INTEGER NOT NULL,
                    PRIMARY KEY (ma_thanh_toan, ma_ve)
                )
                """);
    }

    @Transactional
    public ResponseEntity<?> createPayment(
            CreatePaymentRequest request,
            HttpServletRequest servletRequest,
            HttpSession session) {

        NguoiDung user = (NguoiDung) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập để thanh toán"));
        }

        PendingPayment pendingPayment;

        try {
            pendingPayment = normalizePendingPayment(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }

        double tongTien = 0.0;

        for (PendingSeatGroup group : pendingPayment.groups()) {
            SuatChieu suatChieu = suatChieuRepository.findById(group.maSuatChieu()).orElse(null);
            List<Ghe> gheList = gheRepository.findAllById(group.maGheList());

            if (suatChieu == null || gheList.size() != group.maGheList().size()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Suất chiếu hoặc ghế không tồn tại"));
            }

            for (Ghe ghe : gheList) {
                boolean daDat = veRepository.existsBySuatChieu_MaSuatChieuAndGhe_MaGhe(
                        suatChieu.getMaSuatChieu(),
                        ghe.getMaGhe()
                );

                if (daDat) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Ghế " + ghe.getSoGhe() + " đã được đặt"));
                }
            }

            tongTien += gheList.stream().mapToDouble(this::tinhGiaVe).sum();
        }

        Instant earliestExpiresAt = null;
        List<HeldSeat> heldSeats = new ArrayList<>();

        try {
            for (PendingSeatGroup group : pendingPayment.groups()) {
                for (Integer maGhe : group.maGheList()) {
                    SeatHoldService.SeatHold seatHold = seatHoldService.hold(
                            group.maSuatChieu(),
                            maGhe,
                            user
                    );

                    heldSeats.add(new HeldSeat(group.maSuatChieu(), maGhe));

                    if (earliestExpiresAt == null || seatHold.expiresAt().isBefore(earliestExpiresAt)) {
                        earliestExpiresAt = seatHold.expiresAt();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            releaseHeldSeats(heldSeats);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(user);
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTongTien(tongTien);
        donHang.setTrangThai(false);
        donHang = donHangRepository.save(donHang);

        ThanhToan thanhToan = new ThanhToan();
        thanhToan.setDonHang(donHang);
        thanhToan.setPhuongThuc(getOrCreatePhuongThuc(
                momoProperties.isEnabled() ? PHUONG_THUC_MOMO : PHUONG_THUC_GIA_LAP
        ));
        thanhToan.setTrangThai(false);
        thanhToan.setThoiGianThanhToan(LocalDateTime.now());
        thanhToan = thanhToanRepository.save(thanhToan);

        session.setAttribute(
                PAYMENT_SESSION_PREFIX + thanhToan.getMaThanhToan(),
                pendingPayment
        );
        savePendingPayment(thanhToan.getMaThanhToan(), user.getMaNguoiDung(), pendingPayment);

        String paymentUrl;

        if (momoProperties.isEnabled()) {
            try {
                paymentUrl = createMomoPaymentUrl(
                        thanhToan,
                        pendingPayment,
                        servletRequest
                );
            } catch (IllegalStateException e) {
                releaseHeldSeats(heldSeats);
                return ResponseEntity.internalServerError()
                        .body(new ApiResponse(false, e.getMessage()));
            }
        } else {
            paymentUrl = baseUrl(servletRequest) + "/payment.html?maThanhToan=" + thanhToan.getMaThanhToan();
        }

        return ResponseEntity.ok(new CreatePaymentResponse(
                thanhToan.getMaThanhToan(),
                donHang.getMaDonHang(),
                donHang.getTongTien(),
                paymentUrl,
                earliestExpiresAt != null ? earliestExpiresAt.toEpochMilli() : null
        ));
    }

    public ResponseEntity<?> getPaymentDetail(Integer maThanhToan, HttpSession session) {

        PendingPayment pendingPayment = getPendingPayment(session, maThanhToan);

        if (pendingPayment == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy phiên thanh toán"));
        }

        ThanhToan thanhToan = thanhToanRepository.findById(maThanhToan).orElse(null);

        if (thanhToan == null || thanhToan.getDonHang() == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán"));
        }

        return ResponseEntity.ok(new PaymentDetailResponse(
                thanhToan.getMaThanhToan(),
                thanhToan.getDonHang().getMaDonHang(),
                thanhToan.getDonHang().getTongTien(),
                hienThiTrangThaiThanhToan(thanhToan.getTrangThai()),
                pendingPayment.firstMaSuatChieu(),
                pendingPayment.firstMaGhe(),
                pendingPayment.maGheList(),
                getHoldExpiresAtMillis(session, pendingPayment)
        ));
    }

    @Transactional
    public ResponseEntity<?> confirmSuccess(ConfirmPaymentRequest request, HttpSession session) {

        NguoiDung user = (NguoiDung) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập để thanh toán"));
        }

        PendingPayment pendingPayment = getPendingPayment(session, request.getMaThanhToan());

        if (pendingPayment == null) {
            pendingPayment = getPersistedPendingPayment(request.getMaThanhToan());
        }

        if (pendingPayment == null || !isPendingPaymentOwner(request.getMaThanhToan(), user)) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy phiên thanh toán"));
        }

        ThanhToan thanhToan = thanhToanRepository.findById(request.getMaThanhToan()).orElse(null);

        if (thanhToan == null || thanhToan.getDonHang() == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán"));
        }

        if (Boolean.TRUE.equals(thanhToan.getTrangThai())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Thanh toán này đã hoàn tất"));
        }

        try {
            for (PendingSeatGroup group : pendingPayment.groups()) {
                for (Integer maGhe : group.maGheList()) {
                    seatHoldService.requireHeldByUser(
                            group.maSuatChieu(),
                            maGhe,
                            user
                    );

                    seatHoldService.extendHold(
                            group.maSuatChieu(),
                            maGhe,
                            user,
                            ADMIN_REVIEW_HOLD_DURATION
                    );
                }
            }

            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);

            markPendingPaymentWaitingAdmin(request.getMaThanhToan());

            return ResponseEntity.ok(new ApiResponse(true, "Đã gửi yêu cầu xác nhận thanh toán. Vui lòng chờ admin duyệt vé."));
        } catch (IllegalArgumentException e) {
            thanhToan.setTrangThai(false);
            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    public ResponseEntity<?> getPendingBankPayments() {
        List<Integer> paymentIds = jdbcTemplate.queryForList(
                """
                        SELECT ma_thanh_toan
                        FROM thanh_toan_cho_duyet
                        WHERE trang_thai = ?
                        ORDER BY thoi_gian_yeu_cau ASC
                        """,
                Integer.class,
                TRANG_THAI_CHO_ADMIN
        );

        List<Map<String, Object>> result = new ArrayList<>();

        for (Integer paymentId : paymentIds) {
            ThanhToan thanhToan = thanhToanRepository.findById(paymentId).orElse(null);
            PendingPayment pendingPayment = getPersistedPendingPayment(paymentId);

            if (thanhToan == null || thanhToan.getDonHang() == null || pendingPayment == null) {
                continue;
            }

            DonHang donHang = thanhToan.getDonHang();
            NguoiDung nguoiDung = donHang.getNguoiDung();
            List<SuatChieu> suatChieuList = pendingPayment.groups().stream()
                    .map(group -> suatChieuRepository.findById(group.maSuatChieu()).orElse(null))
                    .filter(item -> item != null)
                    .toList();
            List<Ghe> gheList = pendingPayment.groups().stream()
                    .flatMap(group -> gheRepository.findAllById(group.maGheList()).stream())
                    .toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("maThanhToan", thanhToan.getMaThanhToan());
            item.put("maDonHang", donHang.getMaDonHang());
            item.put("tongTien", donHang.getTongTien());
            item.put("thoiGianYeuCau", getPendingRequestedAt(paymentId));
            item.put("khachHang", nguoiDung != null ? nguoiDung.getTenNguoiDung() : "Khách hàng");
            item.put("email", nguoiDung != null ? nguoiDung.getEmail() : "");
            item.put("phim", suatChieuList.stream()
                    .map(sc -> sc.getPhim() != null ? sc.getPhim().getTenPhim() : "")
                    .filter(tenPhim -> !tenPhim.isBlank())
                    .distinct()
                    .toList());
            item.put("suatChieu", suatChieuList.isEmpty() ? null : suatChieuList.get(0).getThoiGianBatDau());
            item.put("ghe", gheList.stream().map(Ghe::getSoGhe).sorted().toList());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @Transactional
    public ResponseEntity<?> approveBankPayment(Integer maThanhToan) {
        PendingPayment pendingPayment = getPersistedPendingPayment(maThanhToan);

        if (pendingPayment == null || !TRANG_THAI_CHO_ADMIN.equals(getPendingStatus(maThanhToan))) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán chờ duyệt"));
        }

        ThanhToan thanhToan = thanhToanRepository.findById(maThanhToan).orElse(null);

        if (thanhToan == null || thanhToan.getDonHang() == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán"));
        }

        if (Boolean.TRUE.equals(thanhToan.getTrangThai())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Thanh toán này đã hoàn tất"));
        }

        List<VeDTO> veList;
        try {
            veList = emitTickets(pendingPayment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
        savePaymentTickets(maThanhToan, veList);

        DonHang donHang = thanhToan.getDonHang();
        donHang.setTrangThai(true);
        donHangRepository.save(donHang);

        thanhToan.setTrangThai(true);
        thanhToan.setThoiGianThanhToan(LocalDateTime.now());
        thanhToanRepository.save(thanhToan);

        markPendingPaymentProcessed(maThanhToan, TRANG_THAI_DA_DUYET);
        releasePendingPaymentSeats(pendingPayment);

        return ResponseEntity.ok(veList);
    }

    @Transactional
    public ResponseEntity<?> rejectBankPayment(Integer maThanhToan) {
        PendingPayment pendingPayment = getPersistedPendingPayment(maThanhToan);

        if (pendingPayment == null || !TRANG_THAI_CHO_ADMIN.equals(getPendingStatus(maThanhToan))) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán chờ duyệt"));
        }

        markPendingPaymentProcessed(maThanhToan, TRANG_THAI_TU_CHOI);
        releasePendingPaymentSeats(pendingPayment);

        return ResponseEntity.ok(new ApiResponse(true, "Đã từ chối thanh toán và nhả ghế"));
    }

    public ResponseEntity<?> getMyTickets(HttpSession session) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");

        if (user == null || user.getMaNguoiDung() == null) {
            return ResponseEntity.status(401).body(new ApiResponse(false, "Vui lòng đăng nhập để xem vé"));
        }

        List<Integer> ticketIds = jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT tv.ma_ve
                        FROM thanh_toan_ve tv
                        JOIN THANH_TOAN tt ON tt.Ma_Thanh_Toan = tv.ma_thanh_toan
                        JOIN DON_HANG dh ON dh.Ma_Don_Hang = tt.Ma_Don_Hang
                        WHERE dh.Ma_Nguoi_Dung = ?
                          AND tt.Trang_Thai = TRUE
                        ORDER BY tv.ma_ve DESC
                        """,
                Integer.class,
                user.getMaNguoiDung()
        );

        List<VeDTO> tickets = veRepository.findAllById(ticketIds)
                .stream()
                .map(veService::toDTO)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    @Transactional
    public ResponseEntity<String> handleMomoReturn(Map<String, String> params) {

        String htmlPrefix = """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"><title>Kết quả thanh toán</title></head>
                <body style="font-family:Arial;background:#111;color:#fff;padding:30px">
                """;
        String htmlSuffix = """
                <p><a style="color:#ff4d4d" href="/index.html">Quay về trang chủ</a></p>
                </body></html>
                """;

        ResponseEntity<?> result = confirmMomoPayment(params);

        if (result.getStatusCode().is2xxSuccessful()) {
            Object ve = result.getBody();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlPrefix
                            + "<h1>Thanh toán MoMo thành công</h1>"
                            + "<p>Thanh toán đã phát hành vé thành công.</p>"
                            + htmlSuffix);
        }

        Object body = result.getBody();
        String message = body instanceof ApiResponse apiResponse
                ? apiResponse.message()
                : "Thanh toán MoMo thất bại";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlPrefix
                        + "<h1>Thanh toán MoMo thất bại</h1>"
                        + "<p>" + message + "</p>"
                        + htmlSuffix);
    }

    @Transactional
    public ResponseEntity<MomoIpnResponse> handleMomoIpn(Map<String, Object> body) {
        Map<String, String> params = new LinkedHashMap<>();
        body.forEach((key, value) -> params.put(key, value != null ? String.valueOf(value) : ""));

        ResponseEntity<?> result = confirmMomoPayment(params);

        int resultCode = result.getStatusCode().is2xxSuccessful() ? 0 : 1;
        String orderId = params.getOrDefault("orderId", "");
        String requestId = params.getOrDefault("requestId", "");

        return ResponseEntity.ok(new MomoIpnResponse(
                momoProperties.getPartnerCode(),
                orderId,
                requestId,
                resultCode,
                resultCode == 0 ? "Confirm Success" : "Confirm Failed"
        ));
    }

    private ResponseEntity<?> confirmMomoPayment(Map<String, String> params) {

        if (!verifyMomoResultSignature(params)) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Chữ ký MoMo không hợp lệ"));
        }

        if (!"0".equals(params.get("resultCode"))) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Giao dịch MoMo chưa thành công"));
        }

        PendingPayment pendingPayment = decodePendingPayment(params.get("extraData"));
        Integer maThanhToan = extractMaThanhToan(params.get("orderId"));

        if (maThanhToan == null || pendingPayment == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Dữ liệu thanh toán không hợp lệ"));
        }

        ThanhToan thanhToan = thanhToanRepository.findById(maThanhToan).orElse(null);

        if (thanhToan == null || thanhToan.getDonHang() == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán"));
        }

        long paidAmount = parseLong(params.get("amount"));
        long expectedAmount = Math.round(thanhToan.getDonHang().getTongTien());

        if (paidAmount != expectedAmount) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Số tiền thanh toán không khớp"));
        }

        if (Boolean.TRUE.equals(thanhToan.getTrangThai())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Thanh toán này đã hoàn tất"));
        }

        try {
            List<VeDTO> veList = emitTickets(pendingPayment);
            savePaymentTickets(maThanhToan, veList);

            DonHang donHang = thanhToan.getDonHang();
            donHang.setTrangThai(true);
            donHangRepository.save(donHang);

            thanhToan.setTrangThai(true);
            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);
            releasePendingPaymentSeats(pendingPayment);

            return ResponseEntity.ok(veList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private PhuongThucThanhToan getOrCreatePhuongThuc(String tenPhuongThuc) {
        return phuongThucRepository.findByTenPhuongThuc(tenPhuongThuc)
                .orElseGet(() -> {
                    PhuongThucThanhToan phuongThuc = new PhuongThucThanhToan();
                    phuongThuc.setTenPhuongThuc(tenPhuongThuc);
                    return phuongThucRepository.save(phuongThuc);
                });
    }

    private String createMomoPaymentUrl(
            ThanhToan thanhToan,
            PendingPayment pendingPayment,
            HttpServletRequest request) {

        validateMomoConfig();

        String orderId = "TT" + thanhToan.getMaThanhToan() + "-" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().replace("-", "");
        long amount = Math.round(thanhToan.getDonHang().getTongTien());
        String orderInfo = "Thanh toan ve phim don " + thanhToan.getDonHang().getMaDonHang();
        String redirectUrl = firstNonBlank(
                momoProperties.getRedirectUrl(),
                baseUrl(request) + "/payment/momo-return"
        );
        String ipnUrl = firstNonBlank(
                momoProperties.getIpnUrl(),
                baseUrl(request) + "/payment/momo-ipn"
        );
        String extraData = encodePendingPayment(
                thanhToan.getMaThanhToan(),
                pendingPayment
        );
        String requestType = momoProperties.getRequestType();

        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        Map<String, Object> momoRequest = new LinkedHashMap<>();
        momoRequest.put("partnerCode", momoProperties.getPartnerCode());
        momoRequest.put("partnerName", "Cinema Booking");
        momoRequest.put("storeId", "CinemaBooking");
        momoRequest.put("requestId", requestId);
        momoRequest.put("amount", amount);
        momoRequest.put("orderId", orderId);
        momoRequest.put("orderInfo", orderInfo);
        momoRequest.put("redirectUrl", redirectUrl);
        momoRequest.put("ipnUrl", ipnUrl);
        momoRequest.put("lang", "vi");
        momoRequest.put("requestType", requestType);
        momoRequest.put("autoCapture", true);
        momoRequest.put("extraData", extraData);
        momoRequest.put("signature", hmacSha256(rawSignature, momoProperties.getSecretKey()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    momoProperties.getEndpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(momoRequest, headers),
                    Map.class
            );

            Object payUrl = response.getBody() != null ? response.getBody().get("payUrl") : null;
            Object resultCode = response.getBody() != null ? response.getBody().get("resultCode") : null;
            Object message = response.getBody() != null ? response.getBody().get("message") : null;

            if (payUrl == null || !"0".equals(String.valueOf(resultCode))) {
                throw new IllegalStateException("MoMo không tạo được thanh toán: "
                        + (message != null ? message : "Không có payUrl"));
            }

            return String.valueOf(payUrl);
        } catch (RestClientException e) {
            throw new IllegalStateException("Không kết nối được MoMo: " + e.getMessage());
        }
    }

    private boolean verifyMomoResultSignature(Map<String, String> params) {
        String signature = params.get("signature");

        if (signature == null || signature.isBlank()) {
            return false;
        }

        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + params.getOrDefault("amount", "")
                + "&extraData=" + params.getOrDefault("extraData", "")
                + "&message=" + params.getOrDefault("message", "")
                + "&orderId=" + params.getOrDefault("orderId", "")
                + "&orderInfo=" + params.getOrDefault("orderInfo", "")
                + "&orderType=" + params.getOrDefault("orderType", "")
                + "&partnerCode=" + params.getOrDefault("partnerCode", "")
                + "&payType=" + params.getOrDefault("payType", "")
                + "&requestId=" + params.getOrDefault("requestId", "")
                + "&responseTime=" + params.getOrDefault("responseTime", "")
                + "&resultCode=" + params.getOrDefault("resultCode", "")
                + "&transId=" + params.getOrDefault("transId", "");

        return signature.equalsIgnoreCase(hmacSha256(rawSignature, momoProperties.getSecretKey()));
    }

    private String encodePendingPayment(Integer maThanhToan, PendingPayment pendingPayment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maThanhToan", maThanhToan);
        data.put("items", pendingPayment.groups().stream()
                .map(group -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("maSuatChieu", group.maSuatChieu());
                    item.put("maGheList", group.maGheList());
                    return item;
                })
                .toList());

        try {
            return Base64.getEncoder().encodeToString(
                    objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tạo được dữ liệu thanh toán MoMo");
        }
    }

    private PendingPayment decodePendingPayment(String extraData) {
        if (extraData == null || extraData.isBlank()) {
            return null;
        }

        try {
            String json = new String(Base64.getDecoder().decode(extraData), StandardCharsets.UTF_8);
            Map<?, ?> data = objectMapper.readValue(json, Map.class);
            PendingPayment pendingPayment = pendingPaymentFromRawItems(data.get("items"));

            if (pendingPayment != null) {
                return pendingPayment;
            }

            Integer maSuatChieu = asInteger(data.get("maSuatChieu"));
            List<Integer> maGheList = asIntegerList(data.get("maGheList"));

            if (maGheList.isEmpty()) {
                Integer maGhe = asInteger(data.get("maGhe"));

                if (maGhe != null) {
                    maGheList = List.of(maGhe);
                }
            }

            if (maSuatChieu == null || maGheList.isEmpty()) {
                return null;
            }

            return new PendingPayment(List.of(new PendingSeatGroup(maSuatChieu, maGheList)));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractMaThanhToan(String orderId) {
        if (orderId == null || !orderId.startsWith("TT")) {
            return null;
        }

        int dashIndex = orderId.indexOf("-");
        String idText = dashIndex >= 0
                ? orderId.substring(2, dashIndex)
                : orderId.substring(2);

        try {
            return Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String hmacSha256(String data, String secretKey) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmacSha256.init(keySpec);
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không ký được dữ liệu MoMo");
        }
    }

    private void validateMomoConfig() {
        if (isBlank(momoProperties.getPartnerCode())
                || isBlank(momoProperties.getAccessKey())
                || isBlank(momoProperties.getSecretKey())
                || momoProperties.getPartnerCode().startsWith("YOUR_")
                || momoProperties.getAccessKey().startsWith("YOUR_")
                || momoProperties.getSecretKey().startsWith("YOUR_")) {
            throw new IllegalStateException("Chưa cấu hình partnerCode/accessKey/secretKey của MoMo");
        }
    }

    private String firstNonBlank(String configuredValue, String defaultValue) {
        return isBlank(configuredValue) ? defaultValue : configuredValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private List<Integer> asIntegerList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        List<Integer> result = new ArrayList<>();

        for (Object item : values) {
            Integer integer = asInteger(item);

            if (integer != null) {
                result.add(integer);
            }
        }

        return result;
    }

    private PendingPayment normalizePendingPayment(CreatePaymentRequest request) {
        List<PendingSeatGroup> groups = new ArrayList<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CreatePaymentRequest.PaymentSeatGroup item : request.getItems()) {
                if (item == null || item.getMaSuatChieu() == null || item.getMaSuatChieu() <= 0) {
                    continue;
                }

                List<Integer> maGheList = normalizeSeatIds(item.getMaGheList(), null);

                if (!maGheList.isEmpty()) {
                    groups.add(new PendingSeatGroup(item.getMaSuatChieu(), maGheList));
                }
            }
        }

        if (groups.isEmpty()) {
            List<Integer> maGheList = normalizeSeatIds(request.getMaGheList(), request.getMaGhe());

            if (request.getMaSuatChieu() != null && request.getMaSuatChieu() > 0 && !maGheList.isEmpty()) {
                groups.add(new PendingSeatGroup(request.getMaSuatChieu(), maGheList));
            }
        }

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ghế");
        }

        return new PendingPayment(groups);
    }

    private List<Integer> normalizeSeatIds(List<Integer> maGheList, Integer maGhe) {
        LinkedHashSet<Integer> result = new LinkedHashSet<>();

        if (maGheList != null) {
            maGheList.stream()
                    .filter(seatId -> seatId != null && seatId > 0)
                    .forEach(result::add);
        }

        if (maGhe != null && maGhe > 0) {
            result.add(maGhe);
        }

        return new ArrayList<>(result);
    }

    private PendingPayment pendingPaymentFromRawItems(Object value) {
        if (!(value instanceof List<?> rawItems)) {
            return null;
        }

        List<PendingSeatGroup> groups = new ArrayList<>();

        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                continue;
            }

            Integer maSuatChieu = asInteger(item.get("maSuatChieu"));
            List<Integer> maGheList = asIntegerList(item.get("maGheList"));

            if (maSuatChieu != null && !maGheList.isEmpty()) {
                groups.add(new PendingSeatGroup(maSuatChieu, maGheList));
            }
        }

        return groups.isEmpty() ? null : new PendingPayment(groups);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private PendingPayment getPendingPayment(HttpSession session, Integer maThanhToan) {
        if (maThanhToan == null) {
            return null;
        }

        Object value = session.getAttribute(PAYMENT_SESSION_PREFIX + maThanhToan);
        return value instanceof PendingPayment pendingPayment ? pendingPayment : null;
    }

    private void savePendingPayment(Integer maThanhToan, Integer maNguoiDung, PendingPayment pendingPayment) {
        Integer firstMaSuatChieu = pendingPayment.firstMaSuatChieu();

        jdbcTemplate.update("""
                INSERT INTO thanh_toan_cho_duyet
                    (ma_thanh_toan, ma_suat_chieu, ma_nguoi_dung, trang_thai, thoi_gian_tao)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (ma_thanh_toan) DO UPDATE SET
                    ma_suat_chieu = EXCLUDED.ma_suat_chieu,
                    ma_nguoi_dung = EXCLUDED.ma_nguoi_dung,
                    trang_thai = EXCLUDED.trang_thai,
                    thoi_gian_tao = EXCLUDED.thoi_gian_tao,
                    thoi_gian_yeu_cau = NULL,
                    thoi_gian_xu_ly = NULL
                """, maThanhToan, firstMaSuatChieu, maNguoiDung, TRANG_THAI_CHO_KHACH);

        jdbcTemplate.update(
                "DELETE FROM thanh_toan_cho_duyet_ghe WHERE ma_thanh_toan = ?",
                maThanhToan
        );
        jdbcTemplate.update(
                "DELETE FROM thanh_toan_cho_duyet_ghe_chi_tiet WHERE ma_thanh_toan = ?",
                maThanhToan
        );

        for (PendingSeatGroup group : pendingPayment.groups()) {
            for (Integer maGhe : group.maGheList()) {
                jdbcTemplate.update("""
                        INSERT INTO thanh_toan_cho_duyet_ghe_chi_tiet (ma_thanh_toan, ma_suat_chieu, ma_ghe)
                        VALUES (?, ?, ?)
                        ON CONFLICT (ma_thanh_toan, ma_suat_chieu, ma_ghe) DO NOTHING
                        """, maThanhToan, group.maSuatChieu(), maGhe);

                if (group.maSuatChieu().equals(firstMaSuatChieu)) {
                    jdbcTemplate.update("""
                            INSERT INTO thanh_toan_cho_duyet_ghe (ma_thanh_toan, ma_ghe)
                            VALUES (?, ?)
                            ON CONFLICT (ma_thanh_toan, ma_ghe) DO NOTHING
                            """, maThanhToan, maGhe);
                }
            }
        }
    }

    private PendingPayment getPersistedPendingPayment(Integer maThanhToan) {
        if (maThanhToan == null) {
            return null;
        }

        List<Integer> maSuatChieuList = jdbcTemplate.queryForList(
                "SELECT ma_suat_chieu FROM thanh_toan_cho_duyet WHERE ma_thanh_toan = ?",
                Integer.class,
                maThanhToan
        );

        if (maSuatChieuList.isEmpty()) {
            return null;
        }

        List<PendingSeatGroup> groups = jdbcTemplate.query(
                """
                        SELECT ma_suat_chieu, ma_ghe
                        FROM thanh_toan_cho_duyet_ghe_chi_tiet
                        WHERE ma_thanh_toan = ?
                        ORDER BY ma_suat_chieu, ma_ghe
                        """,
                rs -> {
                    Map<Integer, List<Integer>> groupedSeats = new LinkedHashMap<>();

                    while (rs.next()) {
                        groupedSeats.computeIfAbsent(rs.getInt("ma_suat_chieu"), key -> new ArrayList<>())
                                .add(rs.getInt("ma_ghe"));
                    }

                    return groupedSeats.entrySet().stream()
                            .map(entry -> new PendingSeatGroup(entry.getKey(), entry.getValue()))
                            .toList();
                },
                maThanhToan
        );

        if (!groups.isEmpty()) {
            return new PendingPayment(groups);
        }

        List<Integer> maGheList = jdbcTemplate.queryForList(
                """
                        SELECT ma_ghe
                        FROM thanh_toan_cho_duyet_ghe
                        WHERE ma_thanh_toan = ?
                        ORDER BY ma_ghe
                        """,
                Integer.class,
                maThanhToan
        );

        return maGheList.isEmpty()
                ? null
                : new PendingPayment(List.of(new PendingSeatGroup(maSuatChieuList.get(0), maGheList)));
    }

    private boolean isPendingPaymentOwner(Integer maThanhToan, NguoiDung user) {
        if (maThanhToan == null || user == null || user.getMaNguoiDung() == null) {
            return false;
        }

        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM thanh_toan_cho_duyet
                        WHERE ma_thanh_toan = ?
                          AND ma_nguoi_dung = ?
                        """,
                Integer.class,
                maThanhToan,
                user.getMaNguoiDung()
        );

        return count != null && count > 0;
    }

    private void markPendingPaymentWaitingAdmin(Integer maThanhToan) {
        jdbcTemplate.update("""
                UPDATE thanh_toan_cho_duyet
                SET trang_thai = ?,
                    thoi_gian_yeu_cau = CURRENT_TIMESTAMP
                WHERE ma_thanh_toan = ?
                """, TRANG_THAI_CHO_ADMIN, maThanhToan);
    }

    private void markPendingPaymentProcessed(Integer maThanhToan, String status) {
        jdbcTemplate.update("""
                UPDATE thanh_toan_cho_duyet
                SET trang_thai = ?,
                    thoi_gian_xu_ly = CURRENT_TIMESTAMP
                WHERE ma_thanh_toan = ?
                """, status, maThanhToan);
    }

    private String getPendingStatus(Integer maThanhToan) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT trang_thai FROM thanh_toan_cho_duyet WHERE ma_thanh_toan = ?",
                String.class,
                maThanhToan
        );

        return values.isEmpty() ? "" : values.get(0);
    }

    private LocalDateTime getPendingRequestedAt(Integer maThanhToan) {
        List<LocalDateTime> values = jdbcTemplate.queryForList(
                "SELECT thoi_gian_yeu_cau FROM thanh_toan_cho_duyet WHERE ma_thanh_toan = ?",
                LocalDateTime.class,
                maThanhToan
        );

        return values.isEmpty() ? null : values.get(0);
    }

    private void savePaymentTickets(Integer maThanhToan, List<VeDTO> veList) {
        for (VeDTO ve : veList) {
            if (ve == null || ve.getMaVe() == null) {
                continue;
            }

            jdbcTemplate.update("""
                    INSERT INTO thanh_toan_ve (ma_thanh_toan, ma_ve)
                    VALUES (?, ?)
                    ON CONFLICT (ma_thanh_toan, ma_ve) DO NOTHING
                    """, maThanhToan, ve.getMaVe());
        }
    }

    private Double tinhGiaVe(Ghe ghe) {
        String loaiGhe = ghe.getLoaiGhe() != null
                ? ghe.getLoaiGhe().getTenLoaiGhe()
                : "";

        if (loaiGhe.toUpperCase().contains("VIP")) {
            return 120000.0;
        }

        return 90000.0;
    }

    private String baseUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme())
                .append("://")
                .append(request.getServerName());

        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            url.append(":").append(request.getServerPort());
        }

        return url.toString();
    }

    private String hienThiTrangThaiThanhToan(Boolean trangThai) {
        return Boolean.TRUE.equals(trangThai) ? "THANH_CONG" : "CHO_THANH_TOAN";
    }

    private Long getHoldExpiresAtMillis(HttpSession session, PendingPayment pendingPayment) {
        NguoiDung user = (NguoiDung) session.getAttribute("user");

        if (user == null || pendingPayment == null) {
            return null;
        }

        return seatHoldService.getHoldExpiresAtMillis(
                pendingPayment.firstMaSuatChieu(),
                pendingPayment.firstMaGhe(),
                user
        );
    }

    private List<VeDTO> emitTickets(PendingPayment pendingPayment) {
        List<VeDTO> veList = new ArrayList<>();

        for (PendingSeatGroup group : pendingPayment.groups()) {
            SuatChieu suatChieu = suatChieuRepository.findById(group.maSuatChieu()).orElse(null);
            List<Ghe> gheList = gheRepository.findAllById(group.maGheList());

            if (suatChieu == null || gheList.size() != group.maGheList().size()) {
                throw new IllegalArgumentException("Suất chiếu hoặc ghế không tồn tại");
            }

            for (Ghe ghe : gheList) {
                boolean daDat = veRepository.existsBySuatChieu_MaSuatChieuAndGhe_MaGhe(
                        suatChieu.getMaSuatChieu(),
                        ghe.getMaGhe()
                );

                if (daDat) {
                    throw new IllegalArgumentException("Ghế " + ghe.getSoGhe() + " đã được đặt");
                }
            }

            for (Ghe ghe : gheList) {
                veList.add(veService.emitTicket(suatChieu, ghe));
            }
        }

        return veList;
    }

    private void releaseHeldSeats(List<HeldSeat> heldSeats) {
        heldSeats.forEach(heldSeat ->
                seatHoldService.release(heldSeat.maSuatChieu(), heldSeat.maGhe()));
    }

    private void releasePendingPaymentSeats(PendingPayment pendingPayment) {
        pendingPayment.groups().forEach(group ->
                group.maGheList().forEach(maGhe ->
                        seatHoldService.release(group.maSuatChieu(), maGhe)));
    }

    private record PendingPayment(List<PendingSeatGroup> groups) {
        private Integer firstMaSuatChieu() {
            return groups != null && !groups.isEmpty() ? groups.get(0).maSuatChieu() : null;
        }

        private Integer firstMaGhe() {
            return groups != null && !groups.isEmpty() && !groups.get(0).maGheList().isEmpty()
                    ? groups.get(0).maGheList().get(0)
                    : null;
        }

        private List<Integer> maGheList() {
            return groups == null
                    ? List.of()
                    : groups.stream().flatMap(group -> group.maGheList().stream()).toList();
        }
    }

    private record PendingSeatGroup(Integer maSuatChieu, List<Integer> maGheList) {
    }

    private record HeldSeat(Integer maSuatChieu, Integer maGhe) {
    }

    public record ApiResponse(boolean success, String message) {
    }
}
