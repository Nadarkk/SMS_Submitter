package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.UserReqDto;
import com.edafa.sms_submitter.dto.UserResDto;
import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import com.edafa.sms_submitter.mapper.UserMapper;
import com.edafa.sms_submitter.repository.CompanyRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Lazy)
public class UserService implements UserDetailsService {
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final CompanyRepo companyRepo;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;


    //Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResDto createUser(UserReqDto dto) {
        log.info("Creating user with phoneNo: {} for companyId: {}", dto.getPhoneNo(), dto.getCompanyId());
        if (dto.getCompanyId() == null) {
            log.warn("User creation failed: company ID is null");
            throw new IllegalArgumentException("Company ID is required");
        }
        Integer companyId= dto.getCompanyId();
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company with ID {} not found during user creation", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });
        if (userRepo.existsUserByPhoneNoAndCompanyCompanyId(dto.getPhoneNo(), companyId)) {
            log.warn("User creation failed: phone number {} already exists in companyId: {}", dto.getPhoneNo(), companyId);
            throw new DuplicateValueException("User with phone number " + dto.getPhoneNo() + " already exists in the specified company");
        }
        if (userRepo.findUserByFirstNameAndLastNameAndCompanyCompanyId(dto.getFirstName(), dto.getLastName(), companyId) != null) {
            log.warn("User creation failed: name {} {} already exists in companyId: {}", dto.getFirstName(), dto.getLastName(), companyId);
            throw new DuplicateValueException("User with name " + dto.getFirstName() + " " + dto.getLastName() + " already exists in the specified company");
        }
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User newUser = userMapper.toEntity(dto, company, encodedPassword);
        userRepo.save(newUser);
        log.info("User created successfully with ID: {}", newUser.getUserId());
        return userMapper.toDto(newUser);
    }

    //Read — ADMIN or MANAGER (own company only)
    @Transactional(readOnly = true)
    public UserResDto getUserById(Integer userId, Integer requesterId) {
        log.info("Fetching user ID: {} requested by requesterId: {}", userId, requesterId);
        User requester = userRepo.findById(requesterId)
                .orElseThrow(() -> {
                    log.error("Requester with ID {} not found during user fetch", requesterId);
                    return new ResourceNotFoundException("User with ID " + requesterId + " not found");
                });
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.MANAGER) {
            log.warn("Unauthorized user view attempt by requesterId: {}", requesterId);
            throw new IllegalArgumentException("Unauthorized to view user");
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Integer targetCompanyId = (user.getCompany() != null) ? user.getCompany().getCompanyId() : null;
        accessService.checkAccess(requester, targetCompanyId);
        return userMapper.toDto(user);
    }

    //Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResDto updateUser(Integer userId, UserReqDto dto) {
        log.info("Updating user ID: {}", userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during update", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        if (userRepo.existsUserByPhoneNoAndCompanyCompanyIdAndUserIdNot(dto.getPhoneNo(), dto.getCompanyId(), userId)) {
            log.warn("User update failed: phone number {} already exists in companyId: {}", dto.getPhoneNo(), dto.getCompanyId());
            throw new DuplicateValueException("User with phone number " + dto.getPhoneNo() + " already exists in the specified company");
        }
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNo() != null) user.setPhoneNo(dto.getPhoneNo());
        if (dto.getPassword() != null && !dto.getPassword().trim().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getCompanyId() != null) {
            Company company = companyRepo.findById(dto.getCompanyId())
                    .orElseThrow(() -> {
                        log.error("Company with ID {} not found during user update", dto.getCompanyId());
                        return new ResourceNotFoundException("Company with ID " + dto.getCompanyId() + " not found");
                    });
            user.setCompany(company);
        }
        User updatedUser = userRepo.save(user);
        log.info("User ID: {} updated successfully", userId);
        return userMapper.toDto(updatedUser);
    }

    //Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Integer userId) {
        log.info("Deleting user ID: {}", userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during deletion", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        userRepo.delete(user);
        log.info("User ID: {} deleted successfully", userId);
    }
    //for login security
    @Override
    public UserDetails loadUserByUsername(String phoneNo) throws UsernameNotFoundException {
        log.info("Attempting authentication lookup for phoneNo: {}", phoneNo);
        return userRepo.findByPhoneNo(phoneNo)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: phoneNo {} not found", phoneNo);
                    return new ResourceNotFoundException("No user with phone " + phoneNo);
                });
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResDto> getAllUsers (Integer targetCompanyId) {
        //no filter
        log.info("Fetching all users for targetCompanyId: {}", targetCompanyId);
        List<User> users;
        if ( targetCompanyId == null) {
            users= userRepo.findAll();
        }
        else{
            users= userRepo.findAllByCompanyCompanyId(targetCompanyId);}

        log.info("Retrieved {} users", users.size());
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void toggleUserStatus(Integer targetUserId) {
        log.info("Toggling active status for targetUserId: {}", targetUserId);
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> {
                    log.error("User status toggle failed: user ID {} not found", targetUserId);
                    return new IllegalArgumentException("User not found with ID: " + targetUserId);
                });
        // Toggle state: true -> false, false -> true
        user.setIsActive(!user.getIsActive());
        userRepo.save(user);
        log.info("Toggled active status for targetUserId: {} to new status: {}", targetUserId, user.getIsActive());
    }
}