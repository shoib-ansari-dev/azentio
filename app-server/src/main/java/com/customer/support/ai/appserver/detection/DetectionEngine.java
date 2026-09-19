package com.customer.support.ai.appserver.detection;

import com.customer.support.ai.appserver.entity.Transaction;

public interface DetectionEngine {

    void evaluate(Transaction transaction);
}
