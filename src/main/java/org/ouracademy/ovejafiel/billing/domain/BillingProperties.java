package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The plans as the configuration ({@code billing.*}) writes them, with the plans published on the
 * landing page as the defaults, so a price or a maximum changes without touching the code. Every
 * plan binds in place: {@code billing.basic.max-people=200} changes only that value.
 */
@Getter
@Setter
@ConfigurationProperties("billing")
public class BillingProperties {

  private PlanProperties free =
      new PlanProperties(50, 1, 20, "0.50", 0, false, false, false, false, false, "0", "0");
  private PlanProperties basic =
      new PlanProperties(150, 1, 150, "2", 50, false, true, true, true, false, "45", "49000");
  private PlanProperties church =
      new PlanProperties(400, 5, 400, "5", 200, false, true, true, true, true, "75", "79000");
  private PlanProperties network =
      new PlanProperties(
          null, null, 1200, "15", 600, true, false, true, true, true, "149", "159000");

  private OwnNumberProperties ownNumber = new OwnNumberProperties();
  private int yearlyMonthsCharged = 10;
  private int graceDays = 7;

  /** One plan: {@code billing.<plan>.*}. */
  @Getter
  @Setter
  public static class PlanProperties {
    /** Null: no maximum. */
    private Integer maxPeople;

    private Integer maxLocations;
    private int aiMessages;
    private BigDecimal aiMaxCostUsd;
    private int whatsappReminders;
    private boolean ownNumberIncluded;
    private boolean ownNumberExtra;
    private boolean customRoles;
    private boolean excelExport;
    private boolean churchFeatures;
    private BigDecimal pricePe;
    private BigDecimal priceCo;

    public PlanProperties() {}

    PlanProperties(
        Integer maxPeople,
        Integer maxLocations,
        int aiMessages,
        String aiMaxCostUsd,
        int whatsappReminders,
        boolean ownNumberIncluded,
        boolean ownNumberExtra,
        boolean customRoles,
        boolean excelExport,
        boolean churchFeatures,
        String pricePe,
        String priceCo) {
      this.maxPeople = maxPeople;
      this.maxLocations = maxLocations;
      this.aiMessages = aiMessages;
      this.aiMaxCostUsd = new BigDecimal(aiMaxCostUsd);
      this.whatsappReminders = whatsappReminders;
      this.ownNumberIncluded = ownNumberIncluded;
      this.ownNumberExtra = ownNumberExtra;
      this.customRoles = customRoles;
      this.excelExport = excelExport;
      this.churchFeatures = churchFeatures;
      this.pricePe = new BigDecimal(pricePe);
      this.priceCo = new BigDecimal(priceCo);
    }

    BillingPolicy.Limits toLimits() {
      return new BillingPolicy.Limits(
          maxPeople,
          maxLocations,
          aiMessages,
          aiMaxCostUsd,
          whatsappReminders,
          ownNumberIncluded,
          ownNumberExtra,
          customRoles,
          excelExport,
          churchFeatures);
    }
  }

  /** The extra own number: {@code billing.own-number.*}, per month. */
  @Getter
  @Setter
  public static class OwnNumberProperties {
    private BigDecimal pricePe = new BigDecimal("49");
    private BigDecimal priceCo = new BigDecimal("49000");
  }
}
