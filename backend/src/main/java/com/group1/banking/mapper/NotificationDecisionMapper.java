package com.group1.banking.mapper;

import com.group1.banking.dto.response.NotificationDecisionResponse;
import com.group1.banking.entity.NotificationDecisionEntity;
import org.springframework.stereotype.Component;

/**
 * Notification decision mapper. (T073)
 */
@Component
public class NotificationDecisionMapper {

    public NotificationDecisionResponse toResponse(NotificationDecisionEntity entity) {
        NotificationDecisionResponse r = new NotificationDecisionResponse();
        r.setEventId(entity.getEventId());
        r.setDecision(entity.getDecision().name());
        r.setDecisionReason(entity.getDecisionReason());
        r.setCustomerId(entity.getCustomerId());
        r.setAccountId(entity.getAccountId());
        r.setEvaluatedAt(entity.getEvaluatedAt());
        r.setMandatoryOverride(entity.isMandatoryOverride());
        return r;
    }
}
