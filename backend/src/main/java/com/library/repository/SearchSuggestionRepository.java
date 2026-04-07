package com.library.repository;

import com.library.model.SearchSuggestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchSuggestionRepository extends JpaRepository<SearchSuggestion, Long> {

    Optional<SearchSuggestion> findByQuery(String query);

    List<SearchSuggestion> findByQueryStartingWithOrderByFrequencyDesc(String prefix, Pageable pageable);

    List<SearchSuggestion> findByOrderByFrequencyDesc(Pageable pageable);
}
