package com.freelancer.repository;

import com.freelancer.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByClientId(Long clientId);
    List<Project> findByStatus(Project.Status status);

    @Query("SELECT p FROM Project p WHERE LOWER(p.requiredSkills) LIKE LOWER(CONCAT('%', :skill, '%')) AND p.status = 'OPEN'")
    List<Project> findOpenProjectsBySkill(@Param("skill") String skill);

    @Query("SELECT p FROM Project p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Project> findByKeyword(@Param("keyword") String keyword);

    List<Project> findByClientIdAndStatus(Long clientId, Project.Status status);
}
