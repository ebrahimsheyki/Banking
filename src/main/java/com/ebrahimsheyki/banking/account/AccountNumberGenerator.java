package com.ebrahimsheyki.banking.account;

import java.sql.DatabaseMetaData;
import java.util.Locale;
import java.util.Objects;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

  private static final String SEQUENCE_NAME = "account_number_seq";
  private static final String ACCOUNT_NUMBER_FORMAT = "ACC-%010d";

  private final JdbcTemplate jdbcTemplate;
  private final String nextValueSql;

  public AccountNumberGenerator(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.nextValueSql = determineNextValueSql();
  }

  public String generate() {
    Long sequenceValue = jdbcTemplate.queryForObject(nextValueSql, Long.class);
    if (sequenceValue == null) {
      throw new IllegalStateException("Account number sequence returned null.");
    }
    return String.format(Locale.ROOT, ACCOUNT_NUMBER_FORMAT, sequenceValue);
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