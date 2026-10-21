package com.example.document.service;

import com.example.document.entity.DocumentCategory;
import com.example.document.repository.DocumentCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentCategoryService {

    @Autowired
    private DocumentCategoryRepository repository;

    public List<DocumentCategory> getAll() {
        return repository.findAll();
    }

    public DocumentCategory getById(Long id) {
        return repository.findById(id).orElseThrow(() -> 
            new RuntimeException("Category not found"));
    }

    public DocumentCategory create(DocumentCategory category) {
        return repository.save(category);
    }

    public DocumentCategory update(Long id, DocumentCategory category) {
        category.setId(id); 
        return repository.save(category);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}