package com.example.document.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private DocumentCategory category;

    private Long departmentId;

    private String userEmail;

    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "document_translated_titles", 
                     joinColumns = @JoinColumn(name = "document_id"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "translated_title")
    private Map<String, String> translatedTitles = new HashMap<>();

    // Constructors, getters, and setters

    public Document() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getTranslatedTitles() {
        return translatedTitles;
    }

    public void setTranslatedTitles(Map<String, String> translatedTitles) {
        this.translatedTitles = translatedTitles;
    }

    /**
     * Helper method to add a single translated title
     * @param language Language code (e.g., "fr", "es")
     * @param translatedTitle The translated title text
     */
    public void addTranslatedTitle(String language, String translatedTitle) {
        if (this.translatedTitles == null) {
            this.translatedTitles = new HashMap<>();
        }
        this.translatedTitles.put(language, translatedTitle);
    }

    /**
     * Get the translated title for a specific language
     * @param language Language code
     * @return Translated title or null if not available
     */
    public String getTranslatedTitle(String language) {
        if (this.translatedTitles == null) {
            return null;
        }
        return this.translatedTitles.get(language);
    }
}