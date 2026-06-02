package com.cinema.service;

import com.cinema.entity.TheLoai;
import com.cinema.repository.TheLoaiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TheLoaiService {

    @Autowired
    private TheLoaiRepository theLoaiRepository;

    public List<TheLoai> getAll(){
        return theLoaiRepository.findAll();
    }

}