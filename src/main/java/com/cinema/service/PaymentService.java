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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
            ObjectMapper objectMapper) {
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
        this.restTemplate = new RestTemplate();
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

        List<Integer> maGheList;

        try {
            maGheList = normalizeMaGheList(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
        SuatChieu suatChieu = suatChieuRepository.findById(request.getMaSuatChieu()).orElse(null);
        List<Ghe> gheList = gheRepository.findAllById(maGheList);

        if (suatChieu == null || gheList.size() != maGheList.size()) {
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

        Instant earliestExpiresAt = null;
        List<Integer> heldSeats = new ArrayList<>();

        try {
            for (Integer maGhe : maGheList) {
                SeatHoldService.SeatHold seatHold = seatHoldService.hold(
                        request.getMaSuatChieu(),
                        maGhe,
                        user
                );

                heldSeats.add(maGhe);

                if (earliestExpiresAt == null || seatHold.expiresAt().isBefore(earliestExpiresAt)) {
                    earliestExpiresAt = seatHold.expiresAt();
                }
            }
        } catch (IllegalArgumentException e) {
            heldSeats.forEach(maGhe -> seatHoldService.release(request.getMaSuatChieu(), maGhe));
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(user);
        donHang.setThoiGianTao(LocalDateTime.now());
        donHang.setTongTien(gheList.stream().mapToDouble(this::tinhGiaVe).sum());
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
                new PendingPayment(request.getMaSuatChieu(), maGheList)
        );

        String paymentUrl;

        if (momoProperties.isEnabled()) {
            try {
                paymentUrl = createMomoPaymentUrl(
                        thanhToan,
                        request.getMaSuatChieu(),
                        maGheList,
                        servletRequest
                );
            } catch (IllegalStateException e) {
                heldSeats.forEach(maGhe -> seatHoldService.release(request.getMaSuatChieu(), maGhe));
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
                pendingPayment.maSuatChieu(),
                pendingPayment.firstMaGhe(),
                pendingPayment.maGheList(),
                getHoldExpiresAtMillis(session, pendingPayment)
        ));
    }

    @Transactional
    public ResponseEntity<?> confirmSuccess(ConfirmPaymentRequest request, HttpSession session) {

        PendingPayment pendingPayment = getPendingPayment(session, request.getMaThanhToan());

        if (pendingPayment == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy phiên thanh toán"));
        }

        ThanhToan thanhToan = thanhToanRepository.findById(request.getMaThanhToan()).orElse(null);

        if (thanhToan == null || thanhToan.getDonHang() == null) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Không tìm thấy thanh toán"));
        }

        if (Boolean.TRUE.equals(thanhToan.getTrangThai())) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Thanh toán này đã hoàn tất"));
        }

        SuatChieu suatChieu = suatChieuRepository.findById(pendingPayment.maSuatChieu()).orElse(null);
        List<Ghe> gheList = gheRepository.findAllById(pendingPayment.maGheList());

        if (suatChieu == null || gheList.size() != pendingPayment.maGheList().size()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Suất chiếu hoặc ghế không tồn tại"));
        }

        try {
            for (Integer maGhe : pendingPayment.maGheList()) {
                seatHoldService.requireHeldByUser(
                        pendingPayment.maSuatChieu(),
                        maGhe,
                        (NguoiDung) session.getAttribute("user")
                );
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

            List<VeDTO> veList = new ArrayList<>();

            for (Ghe ghe : gheList) {
                veList.add(veService.emitTicket(suatChieu, ghe));
            }

            DonHang donHang = thanhToan.getDonHang();
            donHang.setTrangThai(true);
            donHangRepository.save(donHang);

            thanhToan.setTrangThai(true);
            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);

            session.removeAttribute(PAYMENT_SESSION_PREFIX + request.getMaThanhToan());
            pendingPayment.maGheList().forEach(maGhe ->
                    seatHoldService.release(pendingPayment.maSuatChieu(), maGhe));

            return ResponseEntity.ok(veList);
        } catch (IllegalArgumentException e) {
            thanhToan.setTrangThai(false);
            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
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

        SuatChieu suatChieu = suatChieuRepository.findById(pendingPayment.maSuatChieu()).orElse(null);
        List<Ghe> gheList = gheRepository.findAllById(pendingPayment.maGheList());

        if (suatChieu == null || gheList.size() != pendingPayment.maGheList().size()) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Suất chiếu hoặc ghế không tồn tại"));
        }

        try {
            for (Ghe ghe : gheList) {
                boolean daDat = veRepository.existsBySuatChieu_MaSuatChieuAndGhe_MaGhe(
                        suatChieu.getMaSuatChieu(),
                        ghe.getMaGhe()
                );

                if (daDat) {
                    return ResponseEntity.badRequest().body(new ApiResponse(false, "Ghế " + ghe.getSoGhe() + " đã được đặt"));
                }
            }

            List<VeDTO> veList = new ArrayList<>();

            for (Ghe ghe : gheList) {
                veList.add(veService.emitTicket(suatChieu, ghe));
            }

            DonHang donHang = thanhToan.getDonHang();
            donHang.setTrangThai(true);
            donHangRepository.save(donHang);

            thanhToan.setTrangThai(true);
            thanhToan.setThoiGianThanhToan(LocalDateTime.now());
            thanhToanRepository.save(thanhToan);
            pendingPayment.maGheList().forEach(maGhe ->
                    seatHoldService.release(pendingPayment.maSuatChieu(), maGhe));

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
            Integer maSuatChieu,
            List<Integer> maGheList,
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
                maSuatChieu,
                maGheList
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

    private String encodePendingPayment(Integer maThanhToan, Integer maSuatChieu, List<Integer> maGheList) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maThanhToan", maThanhToan);
        data.put("maSuatChieu", maSuatChieu);
        data.put("maGheList", maGheList);

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

            return new PendingPayment(maSuatChieu, maGheList);
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

    private List<Integer> normalizeMaGheList(CreatePaymentRequest request) {
        LinkedHashSet<Integer> result = new LinkedHashSet<>();

        if (request.getMaGheList() != null) {
            request.getMaGheList().stream()
                    .filter(maGhe -> maGhe != null && maGhe > 0)
                    .forEach(result::add);
        }

        if (request.getMaGhe() != null && request.getMaGhe() > 0) {
            result.add(request.getMaGhe());
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ghế");
        }

        return new ArrayList<>(result);
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
                pendingPayment.maSuatChieu(),
                pendingPayment.firstMaGhe(),
                user
        );
    }

    private record PendingPayment(Integer maSuatChieu, List<Integer> maGheList) {
        private Integer firstMaGhe() {
            return maGheList != null && !maGheList.isEmpty() ? maGheList.get(0) : null;
        }
    }

    public record ApiResponse(boolean success, String message) {
    }
}
