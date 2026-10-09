package com.printwithai.repository;

import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.TemplateDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateDownloadRepository extends JpaRepository<TemplateDownload, Long> {

    long countByTemplate(ApplicationTemplate template);

    List<TemplateDownload> findTop20ByOrderByDownloadedAtDesc();
}
