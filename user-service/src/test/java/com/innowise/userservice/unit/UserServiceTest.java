package com.innowise.userservice.unit;

import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("john.doe@example.com");
        testUser.setPaymentCards(Collections.emptyList());

        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setName("John");
        testUserDto.setSurname("Doe");
        testUserDto.setBirthDate(LocalDate.of(1990, 1, 1));
        testUserDto.setEmail("john.doe@example.com");
    }

    @Test
    void createUserSuccess() {
        UserDto inputDto = new UserDto();
        inputDto.setName("Jane");
        inputDto.setSurname("Smith");

        User unsavedUser = new User();
        unsavedUser.setName("Jane");
        unsavedUser.setSurname("Smith");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName("Jane");
        savedUser.setSurname("Smith");

        UserDto outputDto = new UserDto();
        outputDto.setId(2L);
        outputDto.setName("Jane");
        outputDto.setSurname("Smith");

        when(userMapper.toEntity(inputDto)).thenReturn(unsavedUser);
        when(userRepository.save(unsavedUser)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(outputDto);

        UserDto result = userService.createUser(inputDto);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(userRepository).save(unsavedUser);
        verify(userMapper).toDto(savedUser);
    }

    @Test
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        try (MockedStatic<org.hibernate.Hibernate> mockedHibernate = Mockito.mockStatic(org.hibernate.Hibernate.class)) {
            Optional<UserDto> result = userService.getUserById(1L);

            assertTrue(result.isPresent());
            assertEquals(testUserDto, result.get());
            verify(userRepository).findById(1L);
            verify(userMapper).toDto(testUser);
        }
    }

    @Test
    void getUserByIdNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(1L));

        verify(userRepository).findById(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void getAllUsersWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        String name = "John";
        String surname = "Doe";

        Page<User> userPage = new PageImpl<>(List.of(testUser));

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        Page<UserDto> result = userService.getAllUsers(pageable, name, surname);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testUserDto, result.getContent().getFirst());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAllUsersNoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        Page<UserDto> result = userService.getAllUsers(pageable, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateUserSuccessAllFields() {
        UserDto updatedDto = new UserDto();
        updatedDto.setName("UpdatedName");
        updatedDto.setSurname("UpdatedSurname");
        updatedDto.setBirthDate(LocalDate.of(2000, 1, 1));
        updatedDto.setEmail("updated@email.com");

        User updatedEntity = new User();
        updatedEntity.setName("UpdatedName");
        updatedEntity.setSurname("UpdatedSurname");
        updatedEntity.setBirthDate(LocalDate.of(2000, 1, 1));
        updatedEntity.setEmail("updated@email.com");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("UpdatedName");
        savedUser.setSurname("UpdatedSurname");
        savedUser.setBirthDate(LocalDate.of(2000, 1, 1));
        savedUser.setEmail("updated@email.com");

        UserDto outputDto = new UserDto();
        outputDto.setId(1L);
        outputDto.setName("UpdatedName");
        outputDto.setSurname("UpdatedSurname");
        outputDto.setBirthDate(LocalDate.of(2000, 1, 1));
        outputDto.setEmail("updated@email.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toEntity(updatedDto)).thenReturn(updatedEntity);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(outputDto);

        UserDto result = userService.updateUser(1L, updatedDto);

        assertEquals(outputDto, result);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserSuccessPartialFields() {
        UserDto updatedDto = new UserDto();
        updatedDto.setName("UpdatedName");
        // Other fields null

        User updatedEntity = new User();
        updatedEntity.setName("UpdatedName");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("UpdatedName");
        savedUser.setSurname("Doe"); // unchanged

        UserDto outputDto = new UserDto();
        outputDto.setId(1L);
        outputDto.setName("UpdatedName");
        outputDto.setSurname("Doe");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toEntity(updatedDto)).thenReturn(updatedEntity);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(outputDto);

        UserDto result = userService.updateUser(1L, updatedDto);

        assertEquals("UpdatedName", result.getName());
        assertEquals("Doe", result.getSurname());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserNotFoundThrowsException() {
        UserDto updatedDto = new UserDto();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(1L, updatedDto));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void deleteUserSuccess() {
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }
}
