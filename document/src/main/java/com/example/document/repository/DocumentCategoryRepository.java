package com.example.document.repository;

import com.example.document.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {


    Optional<DocumentCategory> findByName(String name);

    List<DocumentCategory> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT c FROM DocumentCategory c WHERE c.name LIKE %:keyword%")
    List<DocumentCategory> searchByName(@Param("keyword") String keyword);

    long countByName(String name);

    boolean existsByName(String name);
}