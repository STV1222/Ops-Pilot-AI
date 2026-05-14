package com.opspilot.infrastructure.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {
    private static final Logger log = LoggerFactory.getLogger(TemporalConfig.class);
    public static final String TASK_QUEUE = "quotation-comparison";

    @Value("${temporal.target:localhost:7233}")
    private String temporalTarget;

    @Bean
    public WorkflowClient workflowClient() {
        try {
            WorkflowServiceStubsOptions stubOptions = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(temporalTarget)
                    .build();
            WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(stubOptions);
            return WorkflowClient.newInstance(stubs, WorkflowClientOptions.newBuilder().build());
        } catch (Exception e) {
            log.warn("Could not connect to Temporal at {}: {}", temporalTarget, e.getMessage());
            throw e;
        }
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client,
                                       QuotationComparisonActivitiesImpl activitiesImpl) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(QuotationComparisonWorkflowImpl.class);
        worker.registerActivitiesImplementations(activitiesImpl);
        log.info("Temporal worker registered on task queue '{}'", TASK_QUEUE);
        return factory;
    }

    @Bean
    public org.springframework.boot.ApplicationRunner startTemporalWorker(WorkerFactory factory) {
        return args -> {
            factory.start();
            log.info("Temporal worker started, polling '{}'", TASK_QUEUE);
        };
    }
}
