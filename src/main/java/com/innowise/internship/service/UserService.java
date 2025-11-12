package com.innowise.internship.service;

import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.specification.UserSpecification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Page<User> getAllUsers(Pageable pageable, String name, String surname) {
        Specification<User> spec = UserSpecification.hasName(name)
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(spec, pageable);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            if (updatedUser.getName() != null) user.setName(updatedUser.getName());
            if (updatedUser.getSurname() != null) user.setSurname(updatedUser.getSurname());
            if (updatedUser.getBirthDate() != null) user.setBirthDate(updatedUser.getBirthDate());
            if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
            return userRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public void activateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
        });
    }

    @Transactional
    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
    }
}