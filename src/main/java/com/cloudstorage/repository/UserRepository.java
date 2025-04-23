package com.cloudstorage.repository;

import com.cloudstorage.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {
    boolean existsByUsername(String username);

    User getByUsername(String username);
}
