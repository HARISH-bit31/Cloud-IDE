package com.cloudide.cloudide.repository;

import com.cloudide.cloudide.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByProjectIdOrderByIdAsc(Long projectId);
    List<File> findByProjectId(Long projectId);
    Optional<File> findByIdAndProjectId(Long id, Long projectId);
    void deleteByProjectId(Long projectId);
}
