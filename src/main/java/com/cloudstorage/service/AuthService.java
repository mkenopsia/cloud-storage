package com.cloudstorage.service;

import com.cloudstorage.controller.payload.UserPayload;

public interface AuthService {
    void loginUser(UserPayload userPayload);
}
