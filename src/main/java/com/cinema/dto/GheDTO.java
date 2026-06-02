package com.cinema.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GheDTO {
    private Integer maGhe;
    private String soGhe;
    private String loaiGhe;
    private boolean daDat;
    private boolean dangGiu;
    private Long giuDen;

    public GheDTO(Integer maGhe, String soGhe, String loaiGhe, boolean daDat) {
        this(maGhe, soGhe, loaiGhe, daDat, false, null);
    }
}
