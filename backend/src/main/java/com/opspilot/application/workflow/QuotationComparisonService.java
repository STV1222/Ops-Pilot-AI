package com.opspilot.application.workflow;

import com.opspilot.application.document.DocumentExtractor;
import com.opspilot.domain.workflow.WorkflowExecution;
import com.opspilot.domain.workflow.WorkflowStatus;
import com.opspilot.infrastructure.persistence.DocumentRepository;
import com.opspilot.infrastructure.persistence.ReportRepository;
import com.opspilot.infrastructure.persistence.WorkflowExecutionRepository;
import com.opspilot.infrastructure.temporal.QuotationComparisonWorkflow;
import com.opspilot.infrastructure.temporal.TemporalConfig;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;

@Service
public class QuotationComparisonService {
    private static final Logger log = LoggerFactory.getLogger(QuotationComparisonService.class);

    private final List<DocumentExtractor> extractors;
    private final WorkflowExecutionRepository executionRepo;
    private final DocumentRepository documentRepo;
    private final ReportRepository reportRepo;
    private final WorkflowClient workflowClient;

    public QuotationComparisonService(
            List<DocumentExtractor> extractors,
            WorkflowExecutionRepository executionRepo,
            DocumentRepository documentRepo,
            ReportRepository reportRepo,
            WorkflowClient workflowClient) {
        this.extractors = extractors;
        this.executionRepo = executionRepo;
        this.documentRepo = documentRepo;
        this.reportRepo = reportRepo;
        this.workflowClient = workflowClient;
    }

    public Mono<WorkflowExecution> start(UUID organizationId, UUID triggeredBy, Flux<FilePart> files) {
        UUID id = UUID.randomUUID();
        WorkflowExecution running = new WorkflowExecution(
                id, organizationId, "quotation-comparison",
                WorkflowStatus.RUNNING, Instant.now(), null, triggeredBy, Map.of());

        // 1. Persist RUNNING execution to DB
        return Mono.fromCallable(() -> {
            executionRepo.save(running);
            return running;
        })
        .subscribeOn(Schedulers.boundedElastic())
        // 2. Read all file bytes reactively (must stay in request lifecycle)
        .flatMap(exec -> files.flatMap(file -> {
            String contentType = file.headers().getContentType() != null
                    ? file.headers().getContentType().toString() : "";
            String filename = file.filename();
            return file.content()
                    .reduce(new byte[0], (acc, buf) -> {
                        byte[] arr = new byte[buf.readableByteCount()];
                        buf.read(arr);
                        byte[] merged = Arrays.copyOf(acc, acc.length + arr.length);
                        System.arraycopy(arr, 0, merged, acc.length, arr.length);
                        return merged;
                    })
                    .map(content -> {
                        String text = extractors.stream()
                                .filter(ex -> ex.supports(contentType))
                                .findFirst()
                                .map(ex -> ex.extractText(content))
                                .orElse("");
                        return new String[]{ filename, contentType, text };
                    });
        })
        .collectList()
        // 3. Persist documents and start Temporal workflow (on blocking thread)
        .flatMap(docs -> Mono.fromCallable(() -> {
            for (String[] doc : docs) {
                documentRepo.save(organizationId, id, doc[0], doc[1], doc[2]);
            }
            log.info("Saved {} documents for workflow {}", docs.size(), id);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(TemporalConfig.TASK_QUEUE)
                    .setWorkflowId("qc-" + id)
                    .build();
            QuotationComparisonWorkflow stub = workflowClient.newWorkflowStub(
                    QuotationComparisonWorkflow.class, options);
            WorkflowClient.start(stub::run, id);
            log.info("Temporal workflow started for execution {}", id);

            return exec;
        }).subscribeOn(Schedulers.boundedElastic())));
    }

    public List<WorkflowExecution> list() {
        return executionRepo.findAll();
    }

    public Optional<WorkflowExecution> get(UUID id) {
        return executionRepo.findById(id);
    }

    public Optional<Map<String, Object>> getReport(UUID id) {
        return reportRepo.findByExecutionId(id);
    }

    public void approve(UUID id) {
        executionRepo.updateStatus(id, WorkflowStatus.COMPLETED, Instant.now());
        log.info("Workflow {} approved and marked COMPLETED", id);
    }

    public void reject(UUID id) {
        executionRepo.updateStatus(id, WorkflowStatus.FAILED, Instant.now());
        log.info("Workflow {} rejected and marked FAILED", id);
    }
}
