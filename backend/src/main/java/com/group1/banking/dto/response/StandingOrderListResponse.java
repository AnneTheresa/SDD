package com.group1.banking.dto.response;

import java.util.List;

/**
 * Standing order list response DTO. (T048)
 */
public class StandingOrderListResponse {
    private Long accountId;
    private int standingOrderCount;
    private List<StandingOrderResponse> standingOrders;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public int getStandingOrderCount() { return standingOrderCount; }
    public void setStandingOrderCount(int standingOrderCount) { this.standingOrderCount = standingOrderCount; }
    public List<StandingOrderResponse> getStandingOrders() { return standingOrders; }
    public void setStandingOrders(List<StandingOrderResponse> standingOrders) { this.standingOrders = standingOrders; }
}
