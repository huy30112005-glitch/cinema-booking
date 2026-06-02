package com.cinema.service;

import com.cinema.dto.KiemTraVeResponse;
import com.cinema.entity.Ve;
import com.cinema.repository.VeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KiemTraVeService {

    private static final Pattern QR_TICKET_PATTERN =
            Pattern.compile("(?:^|\\|)VE:(\\d+)(?:\\||$)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final VeRepository veRepository;

    public KiemTraVeService(VeRepository veRepository) {
        this.veRepository = veRepository;
    }

    @Transactional(readOnly = true)
    public KiemTraVeResponse traCuu(String maQr) {
        Integer maVe = layMaVeTuQr(maQr);

        Ve ve = veRepository.findById(maVe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé trong hệ thống"));

        if (!Boolean.TRUE.equals(ve.getTrangThai())) {
            return toResponse(
                    ve,
                    false,
                    "DA_SU_DUNG",
                    "Vé này đã được sử dụng trước đó"
            );
        }

        return toResponse(
                ve,
                true,
                "CHUA_SU_DUNG",
                "Vé hợp lệ. Chưa sử dụng"
        );
    }

    @Transactional
    public KiemTraVeResponse xacNhanSuDung(String maQr) {
        Integer maVe = layMaVeTuQr(maQr);

        Ve ve = veRepository.findById(maVe)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé trong hệ thống"));

        if (!Boolean.TRUE.equals(ve.getTrangThai())) {
            return toResponse(
                    ve,
                    false,
                    "DA_SU_DUNG",
                    "Vé này đã được sử dụng trước đó"
            );
        }

        ve.setTrangThai(false);
        ve = veRepository.save(ve);

        return toResponse(
                ve,
                true,
                "DA_XAC_NHAN",
                "Vé hợp lệ. Đã cập nhật trạng thái đã sử dụng"
        );
    }

    private Integer layMaVeTuQr(String maQr) {
        if (maQr == null || maQr.isBlank()) {
            throw new IllegalArgumentException("Vui lòng quét hoặc nhập mã QR");
        }

        String value = maQr.trim();

        if (value.matches("\\d+")) {
            return Integer.valueOf(value);
        }

        Matcher matcher = QR_TICKET_PATTERN.matcher(value);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Mã QR không đúng định dạng vé");
        }

        return Integer.valueOf(matcher.group(1));
    }

    private KiemTraVeResponse toResponse(
            Ve ve,
            boolean hopLe,
            String trangThai,
            String message) {

        return new KiemTraVeResponse(
                hopLe,
                trangThai,
                message,
                ve.getMaVe(),
                ve.getSuatChieu() != null && ve.getSuatChieu().getPhim() != null
                        ? ve.getSuatChieu().getPhim().getTenPhim()
                        : "",
                ve.getGhe() != null ? ve.getGhe().getSoGhe() : "",
                ve.getSuatChieu() != null
                        && ve.getSuatChieu().getPhong() != null
                        && ve.getSuatChieu().getPhong().getTenPhong() != null
                        ? ve.getSuatChieu().getPhong().getTenPhong().getTenPhong()
                        : "",
                ve.getSuatChieu() != null && ve.getSuatChieu().getThoiGianBatDau() != null
                        ? ve.getSuatChieu().getThoiGianBatDau().format(DATE_TIME_FORMATTER)
                        : ""
        );
    }
}
