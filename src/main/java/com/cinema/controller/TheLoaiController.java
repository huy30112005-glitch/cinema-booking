package com.cinema.controller;

import com.cinema.entity.TheLoai;
import com.cinema.service.TheLoaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/theloai")
@CrossOrigin("*")
public class TheLoaiController {

    @Autowired
    private TheLoaiService theLoaiService;

    @GetMapping
    public List<TheLoai> getAll(){
        return theLoaiService.getAll();
    }

}