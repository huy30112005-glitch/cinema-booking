package com.cinema.controller;

import com.cinema.dto.PhongChieuDTO;
import com.cinema.repository.PhongChieuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/phong")
@CrossOrigin("*")
public class PhongChieuController {

    @Autowired
    private PhongChieuRepository phongChieuRepository;

    @GetMapping
    public List<PhongChieuDTO> getAllPhong() {

        return phongChieuRepository.findAll()
                .stream()
                .map(phong -> new PhongChieuDTO(
                        phong.getMaPhong(),
                        phong.getTenPhong() != null ? phong.getTenPhong().getTenPhong() : "",
                        phong.getTongCho()
                ))
                .toList();
    }
}
