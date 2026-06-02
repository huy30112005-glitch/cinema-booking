package com.cinema.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    private final JdbcTemplate jdbcTemplate;

    public AdminReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(29);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        if (to.isBefore(from)) {
            LocalDate temp = from;
            from = to;
            to = temp;
        }

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTimeExclusive = to.plusDays(1).atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", from.toString());
        result.put("toDate", to.toString());
        result.put("summary", getSummary(fromDateTime, toDateTimeExclusive));
        result.put("topMovies", getTopMovies(fromDateTime, toDateTimeExclusive));
        result.put("dailyRevenue", getDailyRevenue(fromDateTime, toDateTimeExclusive));

        return result;
    }

    private Map<String, Object> getSummary(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> summary = jdbcTemplate.queryForObject(
                """
                        SELECT
                            COALESCE(SUM(v.Gia), 0) AS revenue,
                            COUNT(v.Ma_Ve) AS ticket_count,
                            COUNT(DISTINCT tt.Ma_Don_Hang) AS order_count
                        FROM thanh_toan_ve tv
                        JOIN VE v ON v.Ma_Ve = tv.ma_ve
                        JOIN THANH_TOAN tt ON tt.Ma_Thanh_Toan = tv.ma_thanh_toan
                        WHERE tt.Trang_Thai = TRUE
                          AND tt.Thoi_Gian_Thanh_Toan >= ?
                          AND tt.Thoi_Gian_Thanh_Toan < ?
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("revenue", rs.getDouble("revenue"));
                    item.put("ticketCount", rs.getLong("ticket_count"));
                    item.put("orderCount", rs.getLong("order_count"));
                    return item;
                },
                from,
                to
        );

        Long pendingCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM thanh_toan_cho_duyet
                        WHERE trang_thai = 'WAITING_ADMIN'
                        """,
                Long.class
        );

        summary.put("pendingPaymentCount", pendingCount != null ? pendingCount : 0L);
        return summary;
    }

    private List<Map<String, Object>> getTopMovies(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(
                """
                        SELECT
                            p.Ma_Phim AS movie_id,
                            p.Ten_Phim AS movie_name,
                            COUNT(v.Ma_Ve) AS ticket_count,
                            COALESCE(SUM(v.Gia), 0) AS revenue
                        FROM thanh_toan_ve tv
                        JOIN VE v ON v.Ma_Ve = tv.ma_ve
                        JOIN THANH_TOAN tt ON tt.Ma_Thanh_Toan = tv.ma_thanh_toan
                        JOIN SUAT_CHIEU sc ON sc.Ma_Suat_Chieu = v.Ma_Xuat_Chieu
                        JOIN PHIM p ON p.Ma_Phim = sc.Ma_Phim
                        WHERE tt.Trang_Thai = TRUE
                          AND tt.Thoi_Gian_Thanh_Toan >= ?
                          AND tt.Thoi_Gian_Thanh_Toan < ?
                        GROUP BY p.Ma_Phim, p.Ten_Phim
                        ORDER BY ticket_count DESC, revenue DESC
                        LIMIT 5
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("maPhim", rs.getInt("movie_id"));
                    item.put("tenPhim", rs.getString("movie_name"));
                    item.put("soVe", rs.getLong("ticket_count"));
                    item.put("doanhThu", rs.getDouble("revenue"));
                    return item;
                },
                from,
                to
        );
    }

    private List<Map<String, Object>> getDailyRevenue(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(
                """
                        SELECT
                            CAST(tt.Thoi_Gian_Thanh_Toan AS DATE) AS report_date,
                            COUNT(v.Ma_Ve) AS ticket_count,
                            COALESCE(SUM(v.Gia), 0) AS revenue
                        FROM thanh_toan_ve tv
                        JOIN VE v ON v.Ma_Ve = tv.ma_ve
                        JOIN THANH_TOAN tt ON tt.Ma_Thanh_Toan = tv.ma_thanh_toan
                        WHERE tt.Trang_Thai = TRUE
                          AND tt.Thoi_Gian_Thanh_Toan >= ?
                          AND tt.Thoi_Gian_Thanh_Toan < ?
                        GROUP BY CAST(tt.Thoi_Gian_Thanh_Toan AS DATE)
                        ORDER BY report_date DESC
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("ngay", rs.getDate("report_date").toLocalDate().toString());
                    item.put("soVe", rs.getLong("ticket_count"));
                    item.put("doanhThu", rs.getDouble("revenue"));
                    return item;
                },
                from,
                to
        );
    }
}
