package com.opspilot.application.anomaly;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyDetectorService {
    public List<Map<String, Object>> detect(List<Map<String, Object>> items) {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (Map<String, Object> item : items) {
            BigDecimal baseline = new BigDecimal(item.getOrDefault("baselinePrice", "0").toString());
            BigDecimal candidate = new BigDecimal(item.getOrDefault("unitPrice", "0").toString());
            if (baseline.signum() > 0) {
                BigDecimal delta = candidate.subtract(baseline).abs().divide(baseline, 4, java.math.RoundingMode.HALF_UP);
                if (delta.compareTo(new BigDecimal("0.15")) > 0) {
                    anomalies.add(Map.of(
                            "anomalyType", "PRICE_DEVIATION",
                            "severity", "HIGH",
                            "affectedItem", item.getOrDefault("itemName", "unknown"),
                            "explanation", "Unit price deviation exceeds 15% threshold"
                    ));
                }
            }
            if (item.get("quantity") == null) {
                anomalies.add(Map.of(
                        "anomalyType", "MISSING_QUANTITY",
                        "severity", "MEDIUM",
                        "affectedItem", item.getOrDefault("itemName", "unknown"),
                        "explanation", "Missing quantity may break comparability and total cost analysis"
                ));
            }
        }
        return anomalies;
    }
}
