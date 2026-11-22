package com.acxiom.crm.exception;

import com.acxiom.crm.enums.LeadStatus;

public class InvalidLeadStatusTransitionException extends BusinessException {

    public InvalidLeadStatusTransitionException(LeadStatus from, LeadStatus to) {
        super("Invalid lead status transition from " + from + " to " + to);
    }
}
