package com.innowise.userservice.service;

import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.specification.UserSpecification;
import jakarta.transaction.Transactional;
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

    @Transactional
    public Optional<UserDto> getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        user.orElseThrow(() -> new UserNotFoundException(email));
        return user.map(userMapper::toDto);
    }

    @Cacheable(value = "users", key = "#id")
    @Transactional()
    public Optional<UserDto> getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        user.orElseThrow(() -> new UserNotFoundException(id));
        return Optional.ofNullable(userMapper.toDto(user.get()));
    }

    @Transactional
    public Page<UserDto> getAllUsers(Pageable pageable, String name, String surname) {
        Specification<User> spec = UserSpecification.hasName(name)
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserDto updateUser(Long id, UserDto updatedUserDto) {
        return userRepository.findById(id).map(user -> {
            User updatedUser = userMapper.toEntity(updatedUserDto);
            if (updatedUser.getName() != null) user.setName(updatedUser.getName());
            if (updatedUser.getSurname() != null) user.setSurname(updatedUser.getSurname());
            if (updatedUser.getBirthDate() != null) user.setBirthDate(updatedUser.getBirthDate());
            User savedUser = userRepository.save(user);
            return userMapper.toDto(savedUser);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

   /* @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserDto activateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(true);
            User savedUser = userRepository.save(user);
            Hibernate.initialize(savedUser.getPaymentCards());
            return userMapper.toDto(savedUser);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserDto deactivateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(false);
            User savedUser = userRepository.save(user);
            Hibernate.initialize(savedUser.getPaymentCards()); // Ensure full load for consistent caching
            return userMapper.toDto(savedUser);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }*/

}