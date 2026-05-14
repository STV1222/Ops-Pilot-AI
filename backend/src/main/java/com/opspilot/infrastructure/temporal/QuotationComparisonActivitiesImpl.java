package com.opspilot.infrastructure.temporal;

import com.opspilot.application.skill.*;
import com.opspilot.domain.workflow.WorkflowStatus;
import com.opspilot.infrastructure.persistence.DocumentRepository;
import com.opspilot.infrastructure.persistence.ReportRepository;
import com.opspilot.infrastructure.persistence.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class QuotationComparisonActivitiesImpl implements QuotationComparisonActivities {
    private static final Logger log = LoggerFactory.getLogger(QuotationComparisonActivitiesImpl.class);

    private final DocumentRepository documentRepository;
    private final ReportRepository reportRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final SkillRegistry skillRegistry;
    private final SkillExecutor skillExecutor;

    public QuotationComparisonActivitiesImpl(
            DocumentRepository documentRepository,
            ReportRepository reportRepository,
            WorkflowExecutionRepository executionRepository,
            SkillRegistry skillRegistry,
            SkillExecutor skillExecutor) {
        this.documentRepository = documentRepository;
        this.reportRepository = reportRepository;
        this.executionRepository = executionRepository;
        this.skillRegistry = skillRegistry;
        this.skillExecutor = skillExecutor;
    }

    @Override
    public void runSkill(String executionId) {
        UUID id = UUID.fromString(executionId);
        log.info("Temporal activity: runSkill for workflow {}", id);

        try {
            // Load extracted texts from DB (stored during file upload)
            List<String> texts = documentRepository.findExtractedTextByExecutionId(id);
            String combined = String.join("\n\n", texts);

            // Load skill definition (fallback to default if skills dir not mounted)
            SkillDefinition skillDef = skillRegistry.get("quotation-comparison")
                    .orElseGet(() -> new SkillDefinition(
                            "default", "quotation-comparison", "0.1.0",
                            "Compare supplier quotations", ".",
                            Map.of()));

            // Build skill context and execute
            SkillContext context = new SkillContext(
                    id,
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    Map.of("extractedText", combined),
                    Map.of());

            SkillResult result = skillExecutor.execute(skillDef, context);

            if (result.success()) {
                String markdown = (String) result.output().getOrDefault("markdown", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> summary = (Map<String, Object>) result.output().getOrDefault("summary", Map.of());
                reportRepository.save(id, markdown, summary);
                executionRepository.updateStatus(id, WorkflowStatus.PENDING_APPROVAL, null);
                log.info("Workflow {} moved to PENDING_APPROVAL", id);
            } else {
                executionRepository.updateStatus(id, WorkflowStatus.FAILED, Instant.now());
                log.error("Skill execution failed for workflow {}: {}", id, result.message());
            }
        } catch (Exception e) {
            log.error("Activity failed for workflow {}: {}", id, e.getMessage(), e);
            executionRepository.updateStatus(id, WorkflowStatus.FAILED, Instant.now());
            throw e;
        }
    }
}
