package com.group1.banking.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Business Exception for domain-level validation and business rule violations
 * 
 * Maps error codes to appropriate HTTP status codes:
 * - 400: Validation errors (INVALID_TARGET_AMOUNT, INVALID_TARGET_DATE, INVALID_GOAL_NAME, MISSING_REQUIRED_FIELD)
 * - 403: Authorization errors (UNAUTHORIZED_ACCOUNT_ACCESS)
 * - 404: Not found errors (ACCOUNT_NOT_FOUND, GOAL_NOT_FOUND)
 * - 409: Conflict errors (GOAL_ALREADY_EXISTS)
 */
public class BusinessException extends ApiException {
    
    private static final Map<String, Integer> ERROR_CODE_TO_STATUS = new HashMap<>();
    
    static {
        // Validation errors
        ERROR_CODE_TO_STATUS.put("INVALID_TARGET_AMOUNT", 400);
        ERROR_CODE_TO_STATUS.put("INVALID_TARGET_DATE", 400);
        ERROR_CODE_TO_STATUS.put("INVALID_GOAL_NAME", 400);
        ERROR_CODE_TO_STATUS.put("MISSING_REQUIRED_FIELD", 400);
        
        // Authorization errors
        ERROR_CODE_TO_STATUS.put("UNAUTHORIZED_ACCOUNT_ACCESS", 403);
        
        // Not found errors
        ERROR_CODE_TO_STATUS.put("ACCOUNT_NOT_FOUND", 404);
        ERROR_CODE_TO_STATUS.put("GOAL_NOT_FOUND", 404);
        
        // Conflict errors
        ERROR_CODE_TO_STATUS.put("GOAL_ALREADY_EXISTS", 409);
        
        // Internal server error
        ERROR_CODE_TO_STATUS.put("INTERNAL_SERVER_ERROR", 500);
    }
    
    public BusinessException(String code, String message) {
        super(getStatusForCode(code), code, message, null);
    }
    
    public BusinessException(String code, String message, Object details) {
        super(getStatusForCode(code), code, message, details);
    }
    
    private static int getStatusForCode(String code) {
        return ERROR_CODE_TO_STATUS.getOrDefault(code, 500);
    }
}
