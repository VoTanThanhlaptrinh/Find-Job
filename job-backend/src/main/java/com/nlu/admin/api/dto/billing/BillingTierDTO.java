package com.nlu.admin.api.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingTierDTO {
    private String id;
    private String name;
    private String badge;
    private double priceMonthly;
    private String currency;
    private boolean isPopular;
    private int usagePct;
    private List<String> features;
}
