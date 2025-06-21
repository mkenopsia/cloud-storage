package com.cloudstorage.service.UserService;


import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;

import java.util.Optional;

public interface UserService {

    void save(UserPayload user);

    Optional<User> findById(Integer id);

    void deleteById(Integer id);

    boolean isAlreadyExists(UserPayload user);
}
