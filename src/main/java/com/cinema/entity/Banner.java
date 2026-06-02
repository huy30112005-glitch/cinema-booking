package com.cinema.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BANNER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ma_Banner")
    private Integer maBanner;

    @Column(name = "Link_Den")
    private String linkDen;

    @Column(name = "Thu_Tu")
    private Integer thuTu;
}
