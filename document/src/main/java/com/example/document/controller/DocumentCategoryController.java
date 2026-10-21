package com.example.document.controller;

import com.example.document.entity.DocumentCategory;
import com.example.document.service.DocumentCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class DocumentCategoryController {

    @Autowired
    private DocumentCategoryService service;

    @GetMapping
    public List<DocumentCategory> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DocumentCategory getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public DocumentCategory create(@RequestBody DocumentCategory category) {
        return service.create(category);
    }

    @PutMapping("/{id}")
    public DocumentCategory update(@PathVariable Long id, @RequestBody DocumentCategory category) {
        return service.update(id, category);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}