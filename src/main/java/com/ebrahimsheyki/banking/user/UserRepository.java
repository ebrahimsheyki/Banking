package com.ebrahimsheyki.banking.user;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Query("""
      select distinct u
      from User u
      left join fetch u.roles
      where u.username = :username
      """)
  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select u
      from User u
      where u.id = :userId
      """)
  Optional<User> findByIdForUpdate(
      @Param("userId") UUID userId
  );
}