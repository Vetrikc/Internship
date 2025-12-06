package com.innowise.authservice.service;// Updated CustomUserDetailsService - Change findByUsername to findByEmail

import com.innowise.authservice.dao.AuthUserRepository;
import com.innowise.authservice.entity.AuthUser;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.exeption.UserNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    public CustomUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
    @Transactional
    public AuthUser createAuthUser (AuthUser authUser) {
        return authUserRepository.save(authUser);
    }

    @Transactional
    public Boolean existsByEmail (String email) {
        return authUserRepository.existsByEmail(email);
    }

    @Transactional
    public UserDetails deactivateUser(Long id) {
        return authUserRepository.findById(id).map(user -> {
            user.setActive(false);
            return authUserRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }
    @Transactional
    public UserDetails activateUser(Long id) {
        return authUserRepository.findById(id).map(user -> {
            user.setActive(true);
            return authUserRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserDetails setAdminRole(Long id) {
        return authUserRepository.findById(id).map(user -> {
            user.setRole(Role.ADMIN);
            return authUserRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public void deleteAuthUser(Long id) {
        authUserRepository.deleteById(id);
    }

}