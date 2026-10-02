package com.freelancer.service;

import com.freelancer.dto.ProfileRequest;
import com.freelancer.model.FreelancerProfile;
import com.freelancer.model.User;
import com.freelancer.repository.FreelancerProfileRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FreelancerProfileService {

    @Autowired private FreelancerProfileRepository profileRepository;
    @Autowired private UserRepository userRepository;

    public FreelancerProfile createOrUpdateProfile(Long userId, ProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElse(new FreelancerProfile());

        profile.setUser(user);
        profile.setBio(request.getBio());
        profile.setSkills(request.getSkills());
        profile.setExperience(request.getExperience());
        profile.setEducation(request.getEducation());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setPhone(request.getPhone());
        profile.setLocation(request.getLocation());
        profile.setHourlyRate(request.getHourlyRate());

        return profileRepository.save(profile);
    }

    public FreelancerProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    public FreelancerProfile getProfileById(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    public List<FreelancerProfile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public List<FreelancerProfile> searchBySkill(String skill) {
        return profileRepository.findBySkillsContainingIgnoreCase(skill);
    }

    public List<FreelancerProfile> searchByName(String name) {
        return profileRepository.findByUserFullNameContainingIgnoreCase(name);
    }
}
