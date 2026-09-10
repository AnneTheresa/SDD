package com.group1.banking.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.group1.banking.dto.common.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing // handle duplicate keys
                ));

        logger.warn("Validation failed: {}", errors);

        return ResponseEntity.unprocessableContent()
                .body(new ErrorResponse("VALIDATION_FAILED", "Validation failed", errors));
    }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
                logger.warn("Bad request. code={}, message={}", ex.getCode(), ex.getMessage());
                return ResponseEntity.status(400)
                                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
        }
    
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
                logger.warn("Resource Not Found. code={}, message={}", ex.getCode(), ex.getMessage(), ex.getDetails());
                return ResponseEntity.status(404)
                                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
        }

        @ExceptionHandler(UnauthorisedException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorised(UnauthorisedException ex) {
                logger.warn("Unauthorised request. code={}, message={}", ex.getCode(), ex.getMessage());
                return ResponseEntity.status(401)
                                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
        }


        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
            ApiException apiEx = ex;
            logger.warn("Forbidden request. code={}, message={}", apiEx.getCode(), apiEx.getMessage());
            return ResponseEntity.status(403)
                    .body(new ErrorResponse(apiEx.getCode(), apiEx.getMessage(), apiEx.getDetails()));
        }


        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
            ApiException apiEx = ex;
            logger.warn("Conflict occurred. code={}, message={}", apiEx.getCode(), apiEx.getMessage());
            return ResponseEntity.status(409)
                    .body(new ErrorResponse(apiEx.getCode(), apiEx.getMessage(), apiEx.getDetails()));
        }


                @ExceptionHandler(NotFoundException.class)
                public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
                        ApiException apiEx = ex;
                        logger.warn("Resource not found. code={}, message={}", apiEx.getCode(), apiEx.getMessage());
                        return ResponseEntity.status(404)
                                        .body(new ErrorResponse(apiEx.getCode(), apiEx.getMessage(), apiEx.getDetails()));
                }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<ErrorResponse> handleGone(GoneException ex) {
        ApiException apiEx = ex;
        logger.warn("Confirmation gone/expired. code={}, message={}", apiEx.getCode(), apiEx.getMessage());
        return ResponseEntity.status(410)
                .body(new ErrorResponse(apiEx.getCode(), apiEx.getMessage(), apiEx.getDetails()));
    }

    @ExceptionHandler(OwnershipException.class)
    public ResponseEntity<ErrorResponse> handleOwnership(OwnershipException ex) {
        logger.warn("Ownership check failed: {}", ex.getMessage());
        return ResponseEntity.status(403)
                .body(new ErrorResponse("FORBIDDEN", "Access denied.", null));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        logger.warn("Permission denied: {}", ex.getPermission());
        return ResponseEntity.status(403)
                .body(new ErrorResponse("FORBIDDEN", "Access denied.", null)); 
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied.");
        return ResponseEntity.status(403)
                .body(new ErrorResponse("FORBIDDEN", "Access denied.", null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ApiException apiEx = ex;
        logger.warn("Business exception. code={}, message={}, status={}", 
                   apiEx.getCode(), apiEx.getMessage(), apiEx.getStatus());
        return ResponseEntity.status(apiEx.getStatus())
                .body(new ErrorResponse(apiEx.getCode(), apiEx.getMessage(), apiEx.getDetails()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected error occurred", ex);

        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "Something went wrong.", null));
    }
}