package com.cinema.service;

import com.cinema.entity.Phim;
import com.cinema.entity.DinhDang;
import com.cinema.entity.TheLoai;
import com.cinema.repository.PhimRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PhimService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Autowired
    private PhimRepository phimRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public List<Phim> getAllPhim(){
        return phimRepository.findAllWithDetails();
    }

    public List<Phim> getHotPhim(int limit) {
        int size = Math.max(1, Math.min(limit, 12));
        return phimRepository.findHotPhim(PageRequest.of(0, size));
    }

    public Phim savePhim(Phim phim){
        phim.setTrailer(chuanHoaTrailer(phim.getTrailer()));
        return phimRepository.save(phim);
    }

    public Phim savePhim(
            String tenPhim,
            String thoiLuong,
            String moTa,
            Integer maDinhDang,
            Integer maTheLoai,
            String trailer,
            String doTuoi,
            MultipartFile poster) {

        Phim phim = new Phim();
        ganThongTinPhim(phim, tenPhim, thoiLuong, moTa, maDinhDang, maTheLoai, trailer, doTuoi);
        phim.setAnhPoster(luuPoster(poster, null));

        return phimRepository.save(phim);
    }

    public void deletePhim(Integer id){
        phimRepository.deleteById(id);
    }
    public Phim updatePhim(Integer id, Phim phim){

        phim.setMaPhim(id);
        phim.setTrailer(chuanHoaTrailer(phim.getTrailer()));

        return phimRepository.save(phim);

    }

    public Phim updatePhim(
            Integer id,
            String tenPhim,
            String thoiLuong,
            String moTa,
            Integer maDinhDang,
            Integer maTheLoai,
            String trailer,
            String doTuoi,
            MultipartFile poster) {

        Phim phim = phimRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim"));

        ganThongTinPhim(phim, tenPhim, thoiLuong, moTa, maDinhDang, maTheLoai, trailer, doTuoi);
        phim.setAnhPoster(luuPoster(poster, phim.getAnhPoster()));

        return phimRepository.save(phim);
    }

    private void ganThongTinPhim(
            Phim phim,
            String tenPhim,
            String thoiLuong,
            String moTa,
            Integer maDinhDang,
            Integer maTheLoai,
            String trailer,
            String doTuoi) {

        phim.setTenPhim(trimToEmpty(tenPhim));
        phim.setThoiLuong(trimToEmpty(thoiLuong));
        phim.setMoTa(trimToEmpty(moTa));
        phim.setTrailer(chuanHoaTrailer(trailer));
        phim.setDoTuoi(trimToNull(doTuoi));

        if (maDinhDang != null) {
            DinhDang dinhDang = new DinhDang();
            dinhDang.setMaDinhDang(maDinhDang);
            phim.setMaDinhDang(dinhDang);
        } else {
            phim.setMaDinhDang(null);
        }

        if (maTheLoai != null) {
            TheLoai theLoai = new TheLoai();
            theLoai.setMaTheLoai(maTheLoai);
            phim.setMaTheLoai(theLoai);
        } else {
            phim.setMaTheLoai(null);
        }
    }

    private String luuPoster(MultipartFile poster, String currentPath) {
        if (poster == null || poster.isEmpty()) {
            return currentPath;
        }

        String contentType = poster.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Poster phải là file ảnh jpg, png, webp hoặc gif");
        }

        String originalName = poster.getOriginalFilename() != null
                ? poster.getOriginalFilename()
                : "";
        String extension = layDuoiFile(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path uploadPath = Path.of(uploadDir, "image")
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadPath);
            Files.copy(
                    poster.getInputStream(),
                    uploadPath.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh poster");
        }

        return "image/" + fileName;
    }

    private String chuanHoaTrailer(String trailer) {
        String value = trimToNull(trailer);

        if (value == null) {
            return null;
        }

        String videoId = layYoutubeVideoId(value);

        if (videoId != null) {
            return videoId;
        }

        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    private String layYoutubeVideoId(String value) {
        String text = value.trim();

        if (text.matches("^[A-Za-z0-9_-]{11}$")) {
            return text;
        }

        String[] patterns = {
                "youtu.be/",
                "youtube.com/watch?v=",
                "youtube.com/embed/",
                "youtube.com/shorts/"
        };

        for (String pattern : patterns) {
            int index = text.indexOf(pattern);

            if (index >= 0) {
                String id = text.substring(index + pattern.length()).split("[?&#/]")[0];
                return id.matches("^[A-Za-z0-9_-]{11}$") ? id : null;
            }
        }

        return null;
    }

    private String layDuoiFile(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".jpg";
        }

        String extension = fileName.substring(dotIndex).toLowerCase();
        return extension.length() <= 8 ? extension : ".jpg";
    }

    private String trimToEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private String trimToNull(String value) {
        String text = value != null ? value.trim() : "";
        return text.isBlank() ? null : text;
    }
}
