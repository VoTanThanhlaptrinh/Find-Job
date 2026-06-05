package com.nlu.admin.application;

import com.nlu.admin.api.dto.billing.BillingSummaryResponse;
import com.nlu.admin.api.dto.billing.BillingTierDTO;
import com.nlu.admin.api.dto.billing.TransactionListItem;
import com.nlu.shared.domain.model.PageResponse;

import java.util.List;

public interface AdminBillingService {
    List<BillingTierDTO> getBillingTiers();

    void updateBillingTier(String id, BillingTierDTO request);

    PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status);

    BillingSummaryResponse getBillingSummary();
}
