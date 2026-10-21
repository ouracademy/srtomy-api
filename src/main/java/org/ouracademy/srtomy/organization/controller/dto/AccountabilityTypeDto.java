package org.ouracademy.srtomy.organization.controller.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;

@Getter
@AllArgsConstructor
public class AccountabilityTypeDto {
  UUID id;
  String name;
  Boolean isPredefined;

  public static AccountabilityTypeDto from(AccountabilityTypeResult result) {
    return new AccountabilityTypeDto(result.id().value(), result.name(), result.predefined());
  }
}
