package com.printwithai.repository;

import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationTemplateRepository extends JpaRepository<ApplicationTemplate, Long> {

    Optional<ApplicationTemplate> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByCategoryAndActiveTrue(OfficeCategory category);

    Page<ApplicationTemplate> findByActiveTrue(Pageable pageable);

    Page<ApplicationTemplate> findByCategoryAndActiveTrue(OfficeCategory category, Pageable pageable);

    @Query("SELECT t FROM ApplicationTemplate t WHERE t.active = true " +
           "AND (:categorySlug IS NULL OR t.category.slug = :categorySlug) " +
           "AND (:language IS NULL OR t.language = :language) " +
           "AND (:fileType IS NULL OR t.fileType = :fileType) " +
           "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(t.purpose) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ApplicationTemplate> searchTemplates(
            @Param("keyword") String keyword,
            @Param("categorySlug") String categorySlug,
            @Param("language") String language,
            @Param("fileType") String fileType,
            Pageable pageable);

    List<ApplicationTemplate> findTop6ByActiveTrueOrderByDownloadCountDesc();

    List<ApplicationTemplate> findTop8ByActiveTrueOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE ApplicationTemplate t SET t.downloadCount = t.downloadCount + 1 WHERE t.id = :id")
    void incrementDownloadCount(@Param("id") Long id);
}
