package com.cinema.service;

import com.cinema.entity.NguoiDung;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SeatHoldService {

    public static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final Map<SeatKey, SeatHold> holds = new ConcurrentHashMap<>();

    public SeatHold hold(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        cleanupExpired();

        SeatKey key = new SeatKey(maSuatChieu, maGhe);
        Instant now = Instant.now();
        SeatHold existing = holds.get(key);

        if (existing != null && existing.expiresAt().isAfter(now)
                && !existing.userId().equals(user.getMaNguoiDung())) {
            throw new IllegalArgumentException("Ghế này đang được tài khoản khác giữ thanh toán");
        }

        SeatHold hold = new SeatHold(user.getMaNguoiDung(), now.plus(HOLD_DURATION));
        holds.put(key, hold);
        return hold;
    }

    public SeatHold getActiveHold(Integer maSuatChieu, Integer maGhe) {
        SeatKey key = new SeatKey(maSuatChieu, maGhe);
        SeatHold hold = holds.get(key);

        if (hold == null) {
            return null;
        }

        if (!hold.expiresAt().isAfter(Instant.now())) {
            holds.remove(key);
            return null;
        }

        return hold;
    }

    public boolean isHeldByOtherUser(Integer maSuatChieu, Integer maGhe, NguoiDung user) {
        SeatHold hold = getActiveHold(maSuatChieu, maGhe);
        return hold != null && (user == null || !hold.userId().equals(user.getMaNguoiDung()));
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
        SeatHold hold = getActiveHold(maSuatChieu, maGhe);

        if (hold == null || !hold.userId().equals(user.getMaNguoiDung())) {
            return null;
        }

        return hold.expiresAt().toEpochMilli();
    }

    public void release(Integer maSuatChieu, Integer maGhe) {
        holds.remove(new SeatKey(maSuatChieu, maGhe));
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        holds.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record SeatKey(Integer maSuatChieu, Integer maGhe) {
    }

    public record SeatHold(Integer userId, Instant expiresAt) {
    }
}
