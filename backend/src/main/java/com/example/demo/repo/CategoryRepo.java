package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import com.example.demo.domain.Category;

public interface CategoryRepo {

    Optional<List<Category>> findAll();
    Optional<Category> findById(Long id);
    Optional<Category> findByName(String name);
}
