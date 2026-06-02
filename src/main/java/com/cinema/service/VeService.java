package com.cinema.service;

import com.cinema.dto.VeDTO;
import com.cinema.entity.Ghe;
import com.cinema.entity.SuatChieu;
import com.cinema.entity.Ve;
import com.cinema.repository.VeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
public class VeService {

    private final VeRepository veRepository;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public VeService(VeRepository veRepository) {
        this.veRepository = veRepository;
    }

    public VeDTO emitTicket(SuatChieu suatChieu, Ghe ghe) {

        boolean daDat = veRepository.existsBySuatChieu_MaSuatChieuAndGhe_MaGhe(
                suatChieu.getMaSuatChieu(),
                ghe.getMaGhe()
        );

        if (daDat) {
            throw new IllegalArgumentException("Ghế này đã được đặt");
        }

        Ve ve = new Ve();
        ve.setSuatChieu(suatChieu);
        ve.setGhe(ghe);
        ve.setTrangThai(true);
        ve.setGia(tinhGiaVe(ghe));

        String maVeTam = "VE-" + UUID.randomUUID();
        ve.setMaQR(maVeTam.getBytes(StandardCharsets.UTF_8));

        Ve saved = veRepository.save(ve);
        saved.setMaQR(taoAnhQrChoVe(saved));
        saved = veRepository.save(saved);

        return toDTO(saved);
    }

    private VeDTO toDTO(Ve saved) {
        return new VeDTO(
                saved.getMaVe(),
                saved.getGia(),
                saved.getTrangThai(),
                taoDataUrlQr(saved.getMaQR()),
                saved.getGhe().getSoGhe(),
                saved.getSuatChieu().getMaSuatChieu(),
                saved.getSuatChieu().getPhim() != null
                        ? saved.getSuatChieu().getPhim().getTenPhim()
                        : "",
                saved.getSuatChieu().getPhong() != null
                        && saved.getSuatChieu().getPhong().getTenPhong() != null
                        ? saved.getSuatChieu().getPhong().getTenPhong().getTenPhong()
                        : "",
                saved.getSuatChieu().getThoiGianBatDau() != null
                        ? saved.getSuatChieu().getThoiGianBatDau().format(DATE_TIME_FORMATTER)
                        : ""
        );
    }

    private byte[] taoAnhQrChoVe(Ve ve) {
        String noiDungQr = "VE:" + ve.getMaVe()
                + "|PHIM:" + layTenPhim(ve)
                + "|GHE:" + (ve.getGhe() != null ? ve.getGhe().getSoGhe() : "")
                + "|SUAT:" + (ve.getSuatChieu() != null && ve.getSuatChieu().getThoiGianBatDau() != null
                        ? ve.getSuatChieu().getThoiGianBatDau().format(DATE_TIME_FORMATTER)
                        : "")
                + "|GIA:" + Math.round(ve.getGia() != null ? ve.getGia() : 0);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(noiDungQr, BarcodeFormat.QR_CODE, 260, 260);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Không tạo được mã QR cho vé");
        }
    }

    private String taoDataUrlQr(byte[] qrBytes) {
        if (qrBytes == null || qrBytes.length == 0) {
            return "";
        }

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
    }

    private String layTenPhim(Ve ve) {
        return ve.getSuatChieu() != null && ve.getSuatChieu().getPhim() != null
                ? ve.getSuatChieu().getPhim().getTenPhim()
                : "";
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
}

