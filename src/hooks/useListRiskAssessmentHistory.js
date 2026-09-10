import { useQuery } from "@tanstack/react-query";
import { listRiskHistory } from "../api/riskAssessment";

export function useListRiskAssessmentHistory(customerId) {
  return useQuery({
    queryKey: ["customer-risk-history", customerId],
    queryFn: () => listRiskHistory(customerId),
    enabled: Boolean(customerId),
  });
}
