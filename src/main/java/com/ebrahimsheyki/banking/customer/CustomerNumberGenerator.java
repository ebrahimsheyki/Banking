package com.ebrahimsheyki.banking.customer;

import java.sql.DatabaseMetaData;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerNumberGenerator {

  private static final String SEQUENCE_NAME = "customer_number_seq";
  private static final String CUSTOMER_NUMBER_FORMAT = "CUS-%06d";

  private final JdbcTemplate jdbcTemplate;
  private final String nextValueSql;

  public CustomerNumberGenerator(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.nextValueSql = determineNextValueSql();
  }

  public String generate() {
    Long sequenceValue = jdbcTemplate.queryForObject(nextValueSql, Long.class);
    if (sequenceValue == null) {
      throw new IllegalStateException("Customer number sequence returned null");
    }
    return CUSTOMER_NUMBER_FORMAT.formatted(sequenceValue);
  }

  private String determineNextValueSql() {
    return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
      DatabaseMetaData metadata = connection.getMetaData();
      return switch (metadata.getDatabaseProductName()) {
        case "H2" -> "SELECT NEXT VALUE FOR " + SEQUENCE_NAME;
        case "PostgreSQL" -> "SELECT nextVal('" + SEQUENCE_NAME + "')";
        default -> throw new IllegalStateException(
            "Unsupported database: " + metadata.getDatabaseProductName());
      };
    });
  }
}