import { accountApiClient } from "./axiosClient";

export async function listRiskHistory(customerId){
    const response = await accountApiClient.get(`/api/risk_score/customers/${customerId}/history`)
    console.log(response.data)
    return response.data;
}

export async function calculateRiskScore(customerId){
    const response = await accountApiClient.post(`/api/risk_score/customers/${customerId}`)
    console.log(response.data)
    return response.data;
}