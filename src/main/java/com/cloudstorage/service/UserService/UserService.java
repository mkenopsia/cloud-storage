package com.cloudstorage.service.UserService;


import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.entity.User;

import java.util.Optional;

public interface UserService {

    String save(UserPayload user);

    Optional<User> findById(Integer id);

    Optional<User> findByUsername(String username);

    void deleteById(Integer id);

    boolean isUserExists(UserPayload user);
}
