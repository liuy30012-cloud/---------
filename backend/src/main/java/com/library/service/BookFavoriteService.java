package com.library.service;

import com.library.dto.FavoriteResponse;
import com.library.exception.ResourceNotFoundException;
import com.library.model.BookFavorite;
import com.library.repository.BookFavoriteRepository;
import com.library.repository.BookRepository;
import com.library.repository.ReadingStatusRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookFavoriteService {

    private final BookFavoriteRepository bookFavoriteRepository;
    private final BookRepository bookRepository;
    private final ReadingStatusRecordRepository readingStatusRecordRepository;

    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("图书不存在，ID: " + bookId);
        }

        if (bookFavoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            return getFavoriteInfo(userId, bookId);
        }

        BookFavorite favorite = new BookFavorite();
        favorite.setUserId(userId);
        favorite.setBookId(bookId);
        BookFavorite saved = bookFavoriteRepository.save(favorite);
        return convertToResponse(saved);
    }

    @Transactional
    public void removeFavorite(Long userId, Long bookId) {
        if (!bookFavoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new ResourceNotFoundException("未收藏该图书");
        }
        bookFavoriteRepository.deleteByUserIdAndBookId(userId, bookId);
    }

    public boolean isFavorited(Long userId, Long bookId) {
        return bookFavoriteRepository.existsByUserIdAndBookId(userId, bookId);
    }

    public List<FavoriteResponse> getUserFavorites(Long userId) {
        List<BookFavorite> favorites = bookFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return favorites.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<FavoriteResponse> getUserFavorites(Long userId, Pageable pageable) {
        Page<BookFavorite> favorites = bookFavoriteRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return favorites.map(this::convertToResponse);
    }

    public Set<Long> batchCheckFavorites(Long userId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Set.of();
        }
        return bookFavoriteRepository.findFavoritedBookIds(userId, bookIds);
    }

    private FavoriteResponse getFavoriteInfo(Long userId, Long bookId) {
        return bookFavoriteRepository.findByUserIdAndBookId(userId, bookId)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("收藏记录不存在"));
    }

    private FavoriteResponse convertToResponse(BookFavorite favorite) {
        FavoriteResponse response = new FavoriteResponse();
        response.setId(favorite.getId());
        response.setBookId(favorite.getBookId());
        response.setFavoritedAt(favorite.getCreatedAt());

        bookRepository.findById(favorite.getBookId()).ifPresent(book -> {
            response.setBookTitle(book.getTitle());
            response.setAuthor(book.getAuthor());
            response.setCoverUrl(book.getCoverUrl());
            response.setCategory(book.getCategory());
            response.setAvailableCopies(book.getAvailableCopies());
        });

        readingStatusRecordRepository.findByUserIdAndBookId(favorite.getUserId(), favorite.getBookId())
                .ifPresent(status -> {
                    response.setReadingStatus(status.getStatus().name());
                    response.setNotes(status.getNotes());
                });

        return response;
    }
}
