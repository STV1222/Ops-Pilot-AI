package com.opspilot.interfaceadapters.web;

import com.opspilot.application.workflow.QuotationComparisonService;
import com.opspilot.domain.workflow.WorkflowExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {
    private final QuotationComparisonService service;

    public WorkflowController(QuotationComparisonService service) {
        this.service = service;
    }

    @PostMapping("/quotation-comparison")
    public Mono<ResponseEntity<WorkflowExecution>> start(@RequestPart("files") Flux<FilePart> files) {
        UUID orgId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        return service.start(orgId, userId, files).map(ResponseEntity::ok);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowExecution>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowExecution> get(@PathVariable UUID id) {
        return service.get(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<Map<String, Object>> report(@PathVariable UUID id) {
        return service.getReport(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id) {
        service.approve(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id) {
        service.reject(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<Map<String, Object>>> logs(@PathVariable UUID id) {
        return ResponseEntity.ok(List.of(
                Map.of("stage", "extract", "status", "ok"),
                Map.of("stage", "compare", "status", "ok"),
                Map.of("stage", "report", "status", "ok")
        ));
    }
}
