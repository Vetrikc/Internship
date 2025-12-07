package com.innowise.authservice.controller;

import com.innowise.authservice.dto.UserProfileCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8081}")
public interface UserServiceClient {
    @PostMapping("/api/v1/internal/users")
    void createUserProfile(@RequestBody UserProfileCreateRequest request);
}