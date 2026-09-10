package com.group1.banking.dto;

import com.group1.banking.enums.RiskScoreDataElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskScoreFactor {
    private RiskScoreDataElement dataElement; // enum: spending/income ratio, saving balance, saving goals
    private Double weight;
    private Integer subscore;
    private Double contribution;
    private String explanation;
    // Named to match what Jackson derives from the isValid() getter, so the
    // factor round-trips through JSON under a single property name.
    private boolean valid;
}
