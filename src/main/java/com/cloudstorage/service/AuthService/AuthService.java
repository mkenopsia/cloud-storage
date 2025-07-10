package com.cloudstorage.service.AuthService;

import com.cloudstorage.controller.payload.UsernamePayload;

public interface AuthService {
    UsernamePayload getUsernameFromSession();

    Integer getUserIdFromSession();
}
