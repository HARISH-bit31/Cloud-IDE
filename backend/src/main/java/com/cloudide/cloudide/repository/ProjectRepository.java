package com.cloudide.cloudide.repository;

import com.cloudide.cloudide.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Project> findByUserId(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);
}
