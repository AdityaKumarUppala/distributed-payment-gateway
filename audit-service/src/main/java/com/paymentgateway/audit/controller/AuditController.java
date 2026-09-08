package com.paymentgateway.audit.controller;

import com.paymentgateway.audit.entity.AuditEvent;
import com.paymentgateway.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
@Tag(name = "Audit Ledger", description = "Immutable audit log query endpoints for compliance and lifecycle tracing")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Query audit events (optionally filter by aggregate type)")
    public ResponseEntity<List<AuditEvent>> getAuditEvents(
            @RequestParam(value = "aggregateType", required = false) String aggregateType) {
        if (aggregateType != null && !aggregateType.trim().isEmpty()) {
            return ResponseEntity.ok(auditService.getEventsByAggregateType(aggregateType));
        }
        return ResponseEntity.ok(auditService.getAllEvents());
    }

    @GetMapping("/aggregate/{aggregateId}")
    @Operation(summary = "Get full chronological lifecycle audit trail for a specific entity reference")
    public ResponseEntity<List<AuditEvent>> getEventsByAggregateId(@PathVariable("aggregateId") String aggregateId) {
        return ResponseEntity.ok(auditService.getEventsByAggregateId(aggregateId));
    }
}
