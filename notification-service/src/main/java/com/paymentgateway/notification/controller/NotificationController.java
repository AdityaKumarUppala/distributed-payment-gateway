package com.paymentgateway.notification.controller;

import com.paymentgateway.notification.entity.NotificationRecord;
import com.paymentgateway.notification.service.NotificationDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification History", description = "Endpoints to inspect sent notification alerts and simulator logs")
public class NotificationController {

    private final NotificationDispatcherService dispatcherService;

    @GetMapping
    @Operation(summary = "List all dispatched notifications")
    public ResponseEntity<List<NotificationRecord>> getAllNotifications() {
        return ResponseEntity.ok(dispatcherService.getAllNotifications());
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get dispatched notifications for a specific payment/refund reference")
    public ResponseEntity<List<NotificationRecord>> getNotificationsByReference(@PathVariable("reference") String reference) {
        return ResponseEntity.ok(dispatcherService.getNotificationsByReference(reference));
    }
}
