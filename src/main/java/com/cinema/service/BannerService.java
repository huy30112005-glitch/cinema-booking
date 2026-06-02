package com.cinema.service;

import com.cinema.entity.Banner;
import com.cinema.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class BannerService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final BannerRepository bannerRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public BannerService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    public List<Banner> getAllBanner() {
        return bannerRepository.findAllOrderByThuTu();
    }

    public Banner saveBanner(Banner banner) {
        String linkDen = banner.getLinkDen() != null
                ? banner.getLinkDen().trim()
                : "";

        if (linkDen.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập link banner");
        }

        banner.setLinkDen(linkDen);
        return bannerRepository.save(banner);
    }

    public Banner saveBanner(MultipartFile image, Integer thuTu) {
        Banner banner = new Banner();
        banner.setLinkDen(luuAnhBanner(image));
        banner.setThuTu(thuTu);

        return bannerRepository.save(banner);
    }

    public void deleteBanner(Integer id) {
        bannerRepository.deleteById(id);
    }

    private String luuAnhBanner(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh banner");
        }

        String contentType = image.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Banner phải là file ảnh jpg, png, webp hoặc gif");
        }

        String originalName = image.getOriginalFilename() != null
                ? image.getOriginalFilename()
                : "";
        String extension = layDuoiFile(originalName);
        String fileName = "banner-" + UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path uploadPath = Path.of(uploadDir, "image")
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadPath);
            Files.copy(
                    image.getInputStream(),
                    uploadPath.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh banner");
        }

        return "image/" + fileName;
    }

    private String layDuoiFile(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".jpg";
        }

        String extension = fileName.substring(dotIndex).toLowerCase();
        return extension.length() <= 8 ? extension : ".jpg";
    }
}
