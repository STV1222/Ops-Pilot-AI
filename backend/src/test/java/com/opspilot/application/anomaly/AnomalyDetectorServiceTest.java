package com.opspilot.application.anomaly;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AnomalyDetectorServiceTest {
    @Test
    void shouldDetectPriceDeviation() {
        AnomalyDetectorService service = new AnomalyDetectorService();
        var anomalies = service.detect(List.of(Map.of(
                "itemName", "Steel Rod",
                "unitPrice", "130",
                "baselinePrice", "100"
        )));
        assertFalse(anomalies.isEmpty());
    }
}
