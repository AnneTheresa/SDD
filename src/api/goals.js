/**
 * Savings Goal API Wrapper
 *
 * Provides axios functions for all 5 Savings Goal endpoints:
 * - createGoal()
 * - getGoal()
 * - getAllGoals()
 * - updateGoal()
 * - deleteGoal()
 *
 * All functions use the shared axiosClient with JWT auth and error handling
 */

import { accountApiClient as axiosClient } from "./axiosClient";

const GOALS_API = {
  /**
   * Create a new savings goal
   * POST /api/goals/accounts/{account_id}
   *
   * @param {number} accountId - Account ID
   * @param {Object} goalData - Goal data { goalName, targetAmount, targetDate }
   * @returns {Promise<SavingsGoalResponse>}
   */
  createGoal: async (accountId, goalData) => {
    try {
      const response = await axiosClient.post(
        `/api/goals/accounts/${accountId}`,
        {
          goalName: goalData.goalName,
          targetAmount: goalData.targetAmount,
          targetDate: goalData.targetDate,
        },
      );
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get a single savings goal
   * GET /api/goals/accounts/{account_id}
   *
   * @param {number} accountId - Account ID
   * @returns {Promise<SavingsGoalResponse>}
   */
  getGoal: async (accountId) => {
    try {
      const response = await axiosClient.get(
        `/api/goals/accounts/${accountId}`,
      );
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Get all savings goals for a customer (bulk fetch)
   * GET /api/goals/customers/{customer_id}
   *
   * @param {number} customerId - Customer ID
   * @returns {Promise<SavingsGoalResponse[]>}
   */
  getAllGoals: async (customerId) => {
    try {
      const response = await axiosClient.get(
        `/api/goals/customers/${customerId}`,
      );
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Update a savings goal
   * PUT /api/goals/accounts/{account_id}/goals/{goal_id}
   *
   * @param {number} accountId - Account ID
   * @param {number} goalId - Goal ID
   * @param {Object} goalData - Updated goal data { goalName, targetAmount, targetDate }
   * @returns {Promise<SavingsGoalResponse>}
   */
  updateGoal: async (accountId, goalId, goalData) => {
    try {
      const response = await axiosClient.put(
        `/api/goals/accounts/${accountId}/goals/${goalId}`,
        {
          goalName: goalData.goalName,
          targetAmount: goalData.targetAmount,
          targetDate: goalData.targetDate,
        },
      );
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Delete a savings goal (soft delete)
   * DELETE /api/goals/accounts/{account_id}/goals/{goal_id}
   *
   * @param {number} accountId - Account ID
   * @param {number} goalId - Goal ID
   * @returns {Promise<void>}
   */
  deleteGoal: async (accountId, goalId) => {
    try {
      await axiosClient.delete(
        `/api/goals/accounts/${accountId}/goals/${goalId}`,
      );
    } catch (error) {
      throw error;
    }
  },
};

export default GOALS_API;
