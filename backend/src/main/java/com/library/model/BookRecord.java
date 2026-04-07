package com.library.model;

public class BookRecord {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private String location;
    private String coverUrl;
    private String status;
    private String year;
    private String description;
    private String languageCode;
    private String availability;
    private String category;

    public BookRecord() {}

    public BookRecord(String id, String title, String author, String isbn, String location) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.location = location;
        this.status = "Cataloged";
        this.availability = "inLibrary";
    }

    public BookRecord(String id, String title, String author, String isbn, String location,
                     String coverUrl, String status, String year, String description,
                     String languageCode, String availability, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.location = location;
        this.coverUrl = coverUrl;
        this.status = status;
        this.year = year;
        this.description = description;
        this.languageCode = languageCode;
        this.availability = availability;
        this.category = category;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
