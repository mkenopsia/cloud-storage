package com.cloudstorage.service.api;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;

public interface AuthService {
    UsernamePayload getUsernameFromSession();
}
