package com.restoran.pos.service;

import com.restoran.pos.dto.user.PasswordChangeRequest;
import com.restoran.pos.dto.user.UserCreateRequest;
import com.restoran.pos.dto.user.UserPatchRequest;
import com.restoran.pos.dto.user.UserResponse;
import com.restoran.pos.dto.user.UserUpdateRequest;
import com.restoran.pos.security.CurrentUser;

import java.util.List;

public interface UserService {

    List<UserResponse> findAll(String role, Boolean enabled);

    UserResponse findById(Long id);

    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request, CurrentUser actor);

    UserResponse patch(Long id, UserPatchRequest request, CurrentUser actor);

    void changePassword(Long id, PasswordChangeRequest request);

    void delete(Long id, CurrentUser actor);
}
