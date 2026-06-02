package com.cinema.service;

import com.cinema.entity.NguoiDung;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

@Service
public class SeatHoldService {

    public static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final JdbcTemplate jdbcTemplate;

    public SeatHoldService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS GHE_DANG_GIU (
                    Ma_Suat_Chieu INTEGER NOT NULL,
                    Ma_Ghe INTEGER NOT NULL,
                    Ma_Nguoi_Dung INTEGER NOT NULL,
                    Giu_Den TIMESTAMP WITH TIME ZONE NOT NULL,
                    PRIMARY KEY (Ma_Suat_Chieu, Ma_Ghe)
                )
                """);
    }

    public SeatHold hold(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        if (user == null || user.getMaNguoiDung() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thanh toán");
        }

        cleanupExpired();

        Instant expiresAt = Instant.now().plus(HOLD_DURATION);
        int updated = jdbcTemplate.update(
                """
                        INSERT INTO GHE_DANG_GIU (Ma_Suat_Chieu, Ma_Ghe, Ma_Nguoi_Dung, Giu_Den)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (Ma_Suat_Chieu, Ma_Ghe)
                        DO UPDATE SET
                            Ma_Nguoi_Dung = EXCLUDED.Ma_Nguoi_Dung,
                            Giu_Den = EXCLUDED.Giu_Den
                        WHERE GHE_DANG_GIU.Ma_Nguoi_Dung = EXCLUDED.Ma_Nguoi_Dung
                           OR GHE_DANG_GIU.Giu_Den <= CURRENT_TIMESTAMP
                        """,
                maSuatChieu,
                maGhe,
                user.getMaNguoiDung(),
                toOffsetDateTime(expiresAt)
        );

        if (updated == 0) {
            throw new IllegalArgumentException("Ghế này đang được tài khoản khác giữ thanh toán");
        }

        return new SeatHold(user.getMaNguoiDung(), expiresAt);
    }

    public SeatHold getActiveHold(Integer maSuatChieu, Integer maGhe) {
        cleanupExpired();

        return jdbcTemplate.query(
                """
                        SELECT Ma_Nguoi_Dung, Giu_Den
                        FROM GHE_DANG_GIU
                        WHERE Ma_Suat_Chieu = ?
                          AND Ma_Ghe = ?
                          AND Giu_Den > CURRENT_TIMESTAMP
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    return new SeatHold(
                            rs.getInt("Ma_Nguoi_Dung"),
                            rs.getObject("Giu_Den", OffsetDateTime.class).toInstant()
                    );
                },
                maSuatChieu,
                maGhe
        );
    }

    public boolean isHeldByOtherUser(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        SeatHold hold = getActiveHold(maSuatChieu, maGhe);
        return hold != null && (user == null || !hold.userId().equals(user.getMaNguoiDung()));
    }

    public Set<Integer> findHeldSeatIdsByOtherUser(Integer maSuatChieu, NguoiDung user) {
        cleanupExpired();

        if (user == null || user.getMaNguoiDung() == null) {
            return new HashSet<>(jdbcTemplate.queryForList(
                    """
                            SELECT Ma_Ghe
                            FROM GHE_DANG_GIU
                            WHERE Ma_Suat_Chieu = ?
                              AND Giu_Den > CURRENT_TIMESTAMP
                            """,
                    Integer.class,
                    maSuatChieu
            ));
        }

        return new HashSet<>(jdbcTemplate.queryForList(
                """
                        SELECT Ma_Ghe
                        FROM GHE_DANG_GIU
                        WHERE Ma_Suat_Chieu = ?
                          AND Ma_Nguoi_Dung <> ?
                          AND Giu_Den > CURRENT_TIMESTAMP
                        """,
                Integer.class,
                maSuatChieu,
                user.getMaNguoiDung()
        ));
    }

    public void requireHeldByUser(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thanh toán");
        }

        SeatHold hold = getActiveHold(maSuatChieu, maGhe);

        if (hold == null) {
            throw new IllegalArgumentException("Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế");
        }

        if (!hold.userId().equals(user.getMaNguoiDung())) {
            throw new IllegalArgumentException("Ghế này đang được tài khoản khác giữ thanh toán");
        }
    }

    public Long getHoldExpiresAtMillis(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        if (user == null) {
            return null;
        }

        SeatHold hold = getActiveHold(maSuatChieu, maGhe);

        if (hold == null || !hold.userId().equals(user.getMaNguoiDung())) {
            return null;
        }

        return hold.expiresAt().toEpochMilli();
    }

    public void release(Integer maSuatChieu, Integer maGhe) {
        jdbcTemplate.update(
                "DELETE FROM GHE_DANG_GIU WHERE Ma_Suat_Chieu = ? AND Ma_Ghe = ?",
                maSuatChieu,
                maGhe
        );
    }

    private void cleanupExpired() {
        jdbcTemplate.update("DELETE FROM GHE_DANG_GIU WHERE Giu_Den <= CURRENT_TIMESTAMP");
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public record SeatHold(Integer userId, Instant expiresAt) {
    }
}
