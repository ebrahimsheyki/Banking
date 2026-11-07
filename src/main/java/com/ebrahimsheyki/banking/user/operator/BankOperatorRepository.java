package com.ebrahimsheyki.banking.user.operator;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankOperatorRepository
    extends JpaRepository<BankOperator, UUID> {

  Optional<BankOperator> findByUser_Id(UUID userId);

  boolean existsByUser_Id(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("""
      select operator
      from BankOperator operator
      where operator.user.id = :userId
      """)
  Optional<BankOperator> findForAuthorizationByUserId(
      @Param("userId") UUID userId
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select operator
      from BankOperator operator
      where operator.id = :operatorId
      """)
  Optional<BankOperator> findByIdForUpdate(
      @Param("operatorId") UUID operatorId
  );

  @NullMarked
  @Override
  @EntityGraph(attributePaths = "user")
  Page<BankOperator> findAll(Pageable pageable);

  @EntityGraph(attributePaths = "user")
  Page<BankOperator> findAllByStatus(
      BankOperatorStatus status,
      Pageable pageable
  );
}
