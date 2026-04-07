package com.library.repository;

import com.library.model.BookTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookTagRepository extends JpaRepository<BookTag, Long> {

    List<BookTag> findByBookId(Long bookId);

    List<BookTag> findByTagName(String tagName);

    @Query("SELECT DISTINCT bt.tagName FROM BookTag bt WHERE bt.tagType = 'SYSTEM' ORDER BY bt.usageCount DESC")
    List<String> findPopularSystemTags();

    @Query("SELECT bt.bookId FROM BookTag bt WHERE bt.tagName = :tagName")
    List<Long> findBookIdsByTagName(@Param("tagName") String tagName);

    @Query("SELECT bt.tagName, COUNT(bt) as cnt FROM BookTag bt GROUP BY bt.tagName ORDER BY cnt DESC")
    List<Object[]> findTagUsageStatistics();
}
