package com.group1.banking.mapper;

import com.group1.banking.dto.auth.UserResponse;
import com.group1.banking.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRoles(user.getRoles());
        response.setExternalSubjectId(user.getExternalSubjectId());
        response.setCustomerId(user.getCustomerId());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}