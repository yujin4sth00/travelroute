package com.travelroute.backend.user.dto;

import com.travelroute.backend.user.User;

public record UserResponse(
        Long id,
        String nickname,
        String profileImage
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getProfileImage());
    }
}
