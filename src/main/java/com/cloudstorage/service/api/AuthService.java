package com.cloudstorage.service.api;

import com.cloudstorage.controller.payload.UserPayload;

public interface AuthService {
    void loginUser(UserPayload userPayload);
}
