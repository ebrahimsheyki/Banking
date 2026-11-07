package com.ebrahimsheyki.banking.user.operator;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

public record BankOperatorPageResponse(
    List<BankOperatorResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

  public BankOperatorPageResponse {
    content = List.copyOf(
        Objects.requireNonNull(content, "content must not be null")
    );
  }

  public static BankOperatorPageResponse from(
      Page<BankOperator> operators
  ) {

    Objects.requireNonNull(
        operators,
        "operators must not be null"
    );

    Page<BankOperatorResponse> responsePage =
        operators.map(BankOperatorResponse::from);

    return new BankOperatorPageResponse(
        responsePage.getContent(),
        responsePage.getNumber(),
        responsePage.getSize(),
        responsePage.getTotalElements(),
        responsePage.getTotalPages(),
        responsePage.isFirst(),
        responsePage.isLast()
    );
  }
}
