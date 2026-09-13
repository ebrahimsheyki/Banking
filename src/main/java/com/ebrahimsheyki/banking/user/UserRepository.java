package com.ebrahimsheyki.banking.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.username = :username
            """)
    Optional<User> findByUsername(String username);
}