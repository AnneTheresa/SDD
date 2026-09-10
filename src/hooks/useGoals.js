/**
 * useGoals Hook
 *
 * Custom React hook for managing Savings Goal CRUD operations
 * Provides state management for:
 * - goals: Array of fetched goals
 * - currentGoal: Single goal being viewed/edited
 * - loading: Loading state
 * - error: Error state with error code and message
 *
 * Methods:
 * - createGoal(accountId, goalData)
 * - getGoal(accountId)
 * - getAllGoals(customerId)
 * - updateGoal(accountId, goalId, goalData)
 * - deleteGoal(accountId, goalId)
 * - clearError()
 */

import { useState, useCallback } from "react";
import goalsAPI from "../api/goals";

const useGoals = () => {
  const [goals, setGoals] = useState([]);
  const [currentGoal, setCurrentGoal] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  /**
   * Create a new savings goal
   */
  const createGoal = useCallback(async (accountId, goalData) => {
    setLoading(true);
    setError(null);
    try {
      const response = await goalsAPI.createGoal(accountId, goalData);
      setCurrentGoal(response);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Get a single savings goal
   */
  const getGoal = useCallback(async (accountId) => {
    setLoading(true);
    setError(null);
    try {
      const response = await goalsAPI.getGoal(accountId);
      setCurrentGoal(response);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Get all savings goals for a customer
   */
  const getAllGoals = useCallback(async (customerId) => {
    setLoading(true);
    setError(null);
    try {
      const response = await goalsAPI.getAllGoals(customerId);
      setGoals(response);
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update a savings goal
   */
  const updateGoal = useCallback(async (accountId, goalId, goalData) => {
    setLoading(true);
    setError(null);
    try {
      const response = await goalsAPI.updateGoal(accountId, goalId, goalData);
      setCurrentGoal(response);
      // Update goal in list if present
      setGoals((prevGoals) =>
        prevGoals.map((g) => (g.goal_id === goalId ? response : g)),
      );
      return response;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a savings goal (soft delete)
   */
  const deleteGoal = useCallback(
    async (accountId, goalId) => {
      setLoading(true);
      setError(null);
      try {
        await goalsAPI.deleteGoal(accountId, goalId);
        // Remove goal from list
        setGoals((prevGoals) => prevGoals.filter((g) => g.goal_id !== goalId));
        // Clear current goal if it was the deleted one
        if (currentGoal?.goal_id === goalId) {
          setCurrentGoal(null);
        }
      } catch (err) {
        setError(err);
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [currentGoal],
  );

  /**
   * Clear error state
   */
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    goals,
    currentGoal,
    loading,
    error,
    createGoal,
    getGoal,
    getAllGoals,
    updateGoal,
    deleteGoal,
    clearError,
  };
};

export default useGoals;
