package io.github.migrationimpact.demo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

/** Intentionally uses Boot 2-era APIs to exercise Jakarta migration detection. */
@Entity
public class LegacyCustomer {
  @Id @GeneratedValue private Long id;
  @NotBlank private String name;
  public Long getId() { return id; }
  public String getName() { return name; }
}
