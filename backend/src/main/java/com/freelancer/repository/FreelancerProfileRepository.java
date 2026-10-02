package com.freelancer.repository;

import com.freelancer.model.FreelancerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Long> {
    Optional<FreelancerProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    @Query("SELECT fp FROM FreelancerProfile fp WHERE LOWER(fp.skills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<FreelancerProfile> findBySkillsContainingIgnoreCase(@Param("skill") String skill);

    @Query("SELECT fp FROM FreelancerProfile fp JOIN fp.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<FreelancerProfile> findByUserFullNameContainingIgnoreCase(@Param("name") String name);
}
