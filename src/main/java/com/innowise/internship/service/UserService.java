package com.innowise.internship.service;

import com.innowise.internship.dto.UserDto;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.exception.UserNotFoundException;
import com.innowise.internship.mapper.UserMapper;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.specification.UserSpecification;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserDto createUser(UserDto userDto) {
        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id).map(user -> {
            Hibernate.initialize(user.getPaymentCards()); // Force load cards for caching full info
            return userMapper.toDto(user);
        });
    }

    public Page<UserDto> getAllUsers(Pageable pageable, String name, String surname) {
        Specification<User> spec = UserSpecification.hasName(name)
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserDto updateUser(Long id, UserDto updatedUserDto) {
        return userRepository.findById(id).map(user -> {
            User updatedUser = userMapper.toEntity(updatedUserDto);
            if (updatedUser.getName() != null) user.setName(updatedUser.getName());
            if (updatedUser.getSurname() != null) user.setSurname(updatedUser.getSurname());
            if (updatedUser.getBirthDate() != null) user.setBirthDate(updatedUser.getBirthDate());
            if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
            User savedUser = userRepository.save(user);
            return userMapper.toDto(savedUser);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void activateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
        });
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
    }
}