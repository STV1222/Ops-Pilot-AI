package com.opspilot.application.skill;

import com.opspilot.application.ai.AIComparisonService;
import com.opspilot.application.ai.AIExtractionService;
import com.opspilot.application.ai.AIReportGenerationService;
import com.opspilot.application.anomaly.AnomalyDetectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuotationComparisonSkillExecutor implements SkillExecutor {
    private static final Logger log = LoggerFactory.getLogger(QuotationComparisonSkillExecutor.class);

    private final AIExtractionService aiExtractionService;
    private final AnomalyDetectorService anomalyDetectorService;
    private final AIComparisonService aiComparisonService;
    private final AIReportGenerationService aiReportGenerationService;

    public QuotationComparisonSkillExecutor(
            AIExtractionService aiExtractionService,
            AnomalyDetectorService anomalyDetectorService,
            AIComparisonService aiComparisonService,
            AIReportGenerationService aiReportGenerationService) {
        this.aiExtractionService = aiExtractionService;
        this.anomalyDetectorService = anomalyDetectorService;
        this.aiComparisonService = aiComparisonService;
        this.aiReportGenerationService = aiReportGenerationService;
    }

    @Override
    public SkillResult execute(SkillDefinition skillDef, SkillContext context) {
        try {
            log.info("Executing skill '{}' for workflow {}", skillDef.skillName(), context.workflowExecutionId());
            String extractedText = (String) context.input().getOrDefault("extractedText", "");

            // Step: extract structured line items from raw text
            List<Map<String, Object>> items = aiExtractionService.extractStructuredQuotationItems(extractedText);
            log.info("Extracted {} line items", items.size());

            // Step: rule-based anomaly detection
            List<Map<String, Object>> anomalies = anomalyDetectorService.detect(items);
            log.info("Detected {} anomalies", anomalies.size());

            // Step: AI-powered supplier comparison
            Map<String, Object> comparison = aiComparisonService.compare(items);

            // Step: generate final markdown report
            Map<String, Object> report = aiReportGenerationService.generateReport(comparison, anomalies);

            Map<String, Object> output = new HashMap<>(report);
            output.put("anomalies", anomalies);
            output.put("comparison", comparison);
            output.put("anomalyCount", anomalies.size());

            return new SkillResult(true, output, "Quotation comparison completed successfully");
        } catch (Exception e) {
            log.error("Skill execution failed: {}", e.getMessage(), e);
            return new SkillResult(false, Map.of(), "Skill execution failed: " + e.getMessage());
        }
    }
}
