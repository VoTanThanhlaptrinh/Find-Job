package com.nlu.admin.api;

import com.nlu.admin.api.dto.billing.BillingSummaryResponse;
import com.nlu.admin.api.dto.billing.BillingTierDTO;
import com.nlu.admin.api.dto.billing.TransactionListItem;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.domain.model.PageResponse;
import com.nlu.admin.application.AdminService;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/billing", produces = "application/json")
@RequiredArgsConstructor
public class AdminBillingController {
    private final AdminService adminService;

    @GetMapping("/tiers")
    public ResponseEntity<ApiResponse<List<BillingTierDTO>>> getTiers() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getBillingTiers(), 
                HttpStatus.OK.value()
        ));
    }

    @PatchMapping("/tiers/{id}")
    public ResponseEntity<ApiResponse<Boolean>> updateTier(@PathVariable String id, @RequestBody BillingTierDTO request) {
        adminService.updateBillingTier(id, request);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                true, 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<TransactionListItem>>> getTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getTransactions(page, pageSize, status), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BillingSummaryResponse>> getSummary() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getBillingSummary(), 
                HttpStatus.OK.value()
        ));
    }
}
