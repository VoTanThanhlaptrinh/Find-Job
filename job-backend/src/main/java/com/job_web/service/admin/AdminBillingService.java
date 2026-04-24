package com.job_web.service.admin;

import com.job_web.dto.admin.billing.BillingSummaryResponse;
import com.job_web.dto.admin.billing.BillingTierDTO;
import com.job_web.dto.admin.billing.TransactionListItem;
import com.job_web.dto.common.PageResponse;

import java.util.List;

public interface AdminBillingService {
    List<BillingTierDTO> getBillingTiers();

    void updateBillingTier(String id, BillingTierDTO request);

    PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status);

    BillingSummaryResponse getBillingSummary();
}
