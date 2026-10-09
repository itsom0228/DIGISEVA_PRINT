package com.printwithai.repository;

import com.printwithai.entity.OfficeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeCategoryRepository extends JpaRepository<OfficeCategory, Long> {

    Optional<OfficeCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<OfficeCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<OfficeCategory> findAllByOrderByDisplayOrderAsc();
}
