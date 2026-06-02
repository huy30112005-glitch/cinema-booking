package com.cinema.service;

import com.cinema.entity.DinhDang;
import com.cinema.repository.DinhDangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DinhDangService {

    @Autowired
    private DinhDangRepository dinhDangRepository;

    public List<DinhDang> getAll(){
        return dinhDangRepository.findAll();
    }

}