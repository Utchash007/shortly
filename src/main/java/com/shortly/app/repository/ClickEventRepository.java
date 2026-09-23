package com.shortly.app.repository;

import com.shortly.app.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Persistence and aggregation queries for {@link ClickEvent} records.
 *
 * <p>Each aggregation returns raw {@code Object[]} rows mapped by the analytics
 * service: {@code [0]} grouping key, {@code [1]} click count.
 */
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /**
     * Counts all recorded clicks for a link.
     *
     * @param urlId the owning link id
     * @return total click count
     */
    long countByUrl_Id(Long urlId);

    /**
     * Counts clicks per calendar date for a link, oldest first.
     *
     * @param urlId the owning link id
     * @return rows of {@code [java.sql.Date, Long]}
     */
    @Query("SELECT FUNCTION('date', c.clickedAt), COUNT(c) FROM ClickEvent c "
            + "WHERE c.url.id = :urlId "
            + "GROUP BY FUNCTION('date', c.clickedAt) ORDER BY 1")
    List<Object[]> countGroupedByDate(@Param("urlId") Long urlId);

    /**
     * Counts clicks per country for a link, most clicks first.
     *
     * @param urlId the owning link id
     * @return rows of {@code [String, Long]}
     */
    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c "
            + "WHERE c.url.id = :urlId "
            + "GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countGroupedByCountry(@Param("urlId") Long urlId);
}
