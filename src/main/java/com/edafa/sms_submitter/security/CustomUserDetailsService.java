package com.edafa.sms_submitter.security;

import com.edafa.sms_submitter.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNo) throws UsernameNotFoundException {
        return userRepository.findByPhoneNo(phoneNo)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone number: " + phoneNo));
    }
}