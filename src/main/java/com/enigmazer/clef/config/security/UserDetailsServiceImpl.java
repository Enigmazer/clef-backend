package com.enigmazer.clef.config.security;

import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String roleName = "ROLE_" + user.getRole().getName().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        // OAuth2 only users might not have a password
        // empty string satisfies Spring Security's non-null requirement
        String safePassword = user.getPassword() == null ? "" : user.getPassword();

        log.debug("Successfully loaded user details from database [userId={}]", user.getId());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                safePassword,
                user.isActive(),
                Collections.singletonList(authority)
        );
    }
}
