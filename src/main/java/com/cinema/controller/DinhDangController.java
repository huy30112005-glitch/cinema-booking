package com.cinema.controller;

import com.cinema.entity.DinhDang;
import com.cinema.service.DinhDangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dinhdang")
@CrossOrigin("*")
public class DinhDangController {

    @Autowired
    private DinhDangService dinhDangService;

    @GetMapping
    public List<DinhDang> getAll(){
        return dinhDangService.getAll();
    }

}