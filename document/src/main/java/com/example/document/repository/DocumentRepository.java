package com.example.document.repository;

import com.example.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByTitleContainingIgnoreCase(String title);
    List<Document> findByCategory_Id(Long categoryId);
    List<Document> findByDepartmentId(Long departmentId);
}
