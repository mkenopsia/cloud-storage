package com.cloudstorage.repository;

import com.cloudstorage.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Integer> {
    boolean existsByUsername(String username);

    Optional<User> getByUsername(String username);

    Optional<User> findByUsername(String username);
}
