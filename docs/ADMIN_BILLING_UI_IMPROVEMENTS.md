# Kế hoạch cải thiện UI — Admin Billing

> Route: `/admin/billing`  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §3, §5, §6.10, §8–10 và `DESIGN.md`  
> Phạm vi frontend: `job-frontend/src/app/features/admin/pages/billing/`  
> Trạng thái capability: **Available** với summary, tiers, update tier và transactions

## 1. Mục tiêu và giá trị mang lại

Billing là màn hình vận hành doanh thu và gói dịch vụ. UI cần giúp quản trị viên:

1. Nhận biết nhanh MRR, tăng trưởng và số thuê bao hoạt động.
2. So sánh các gói dịch vụ, mức sử dụng và trạng thái popular.
3. Chỉnh giá/quyền lợi an toàn, hiểu rõ thời điểm áp dụng.
4. Tra cứu giao dịch và nhận biết giao dịch thất bại mà không dựa riêng vào màu sắc.

Thiết kế phải phân biệt rõ dữ liệu đang có với các capability chưa có contract. Không tạo revenue chart, subscription mix, invoice, refund hoặc transaction detail bằng dữ liệu giả.

## 2. Hiện trạng và khoảng cách cần xử lý

| Khu vực | Hiện trạng | Khoảng cách với đặc tả | Giá trị sau cải thiện |
|---|---|---|---|
| Header | Tiêu đề tiếng Anh, mô tả không dấu | Không nhất quán tiếng Việt; date range chưa có contract | Bối cảnh rõ ràng và không tạo control không hoạt động |
| Summary | Có 3 KPI dạng card cơ bản | Thiếu hierarchy, trend context, skeleton đúng cấu trúc và scoped error | Đọc tình trạng doanh thu nhanh hơn |
| Revenue chart | Chưa có dữ liệu/API | Wireframe có chart nhưng API hiện tại không cung cấp time series | Ghi nhận dependency, không dựng chart giả |
| Subscription mix | Chưa có dữ liệu/API | Không có breakdown theo tier | Chỉ triển khai sau khi có aggregate contract |
| Tiers | Có card nhưng form edit nằm trực tiếp trong từng card | Mật độ cao, dễ sửa nhầm, thiếu validation và thời điểm áp dụng | Card chỉ đọc rõ ràng; chỉnh sửa tập trung trong drawer |
| Giá tiền | Nối `number + currency` thủ công | Không đáp ứng locale/currency formatting | Hiển thị đúng ký hiệu, phân cách và precision |
| Transactions | Có status filter và table | Thiếu caption/scope, badge semantic, pagination, date formatting, scoped error | Dễ quét, lọc và điều hướng dữ liệu lớn |
| Async state | Loading là text, lỗi chỉ qua toast | Không giữ dữ liệu/form khi request lỗi | Người dùng tự retry và không mất nội dung đang chỉnh |
| Responsive/a11y | Chưa có acceptance rõ | Form card dài, action nhỏ, table thiếu semantics | Dùng tốt trên mobile, keyboard và screen reader |

## 3. Bố cục đích

### Với contract hiện tại

```text
[Breadcrumb + Billing]

[MRR] [Tăng trưởng MRR] [Thuê bao hoạt động]

[Gói dịch vụ]
[Tier card] [Tier card] [Tier card]  ->  [Edit drawer]

[Giao dịch]                                      [Status filter]
[Employer | Package | Amount | Date | Status]
[Total records]                    [Pagination]
```

### Khi có API mở rộng

```text
[Header] [Date range]
[Revenue trend 2/3] [Subscription mix 1/3]
```

Date range, revenue trend và subscription mix không được hiển thị như chức năng hoạt động trước khi có API aggregate tương ứng.

## 4. Danh sách nhiệm vụ triển khai

### P0 — Luồng vận hành bắt buộc

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| BL-01 | Chuẩn hóa toàn bộ copy sang tiếng Việt có dấu | Nhất quán với Admin Portal | Không còn `Billing`, `Tiers`, `Saving`, `Dang tai`, `Khong co`; dùng thuật ngữ thống nhất |
| BL-02 | Chuẩn hóa 3 KPI summary | Đọc nhanh tình trạng doanh thu | Có MRR, tăng trưởng MRR, thuê bao hoạt động; số dùng tabular numerals |
| BL-03 | Format tiền bằng locale formatter | Tránh sai ký hiệu và định dạng | Dùng `Intl.NumberFormat` hoặc Angular CurrencyPipe; lấy currency từ API; không nối chuỗi thủ công |
| BL-04 | Chuyển edit tier sang drawer | Giảm sửa nhầm và giảm nhiễu trên card | Card ở trạng thái read-only; “Chỉnh sửa gói” mở drawer 480px desktop/full-screen mobile |
| BL-05 | Tạo typed form cho tier | Validation và error handling rõ ràng | `priceMonthly > 0`; features được chuẩn hóa; inline error; submit disable khi invalid/loading |
| BL-06 | Nêu rõ thời điểm áp dụng | Quản trị viên hiểu tác động thay đổi | Drawer hiển thị rule thực từ backend; nếu contract chưa có thì ghi dependency, không tự suy diễn immediate/proration |
| BL-07 | Giữ form khi update lỗi | Không mất dữ liệu người dùng | Drawer không đóng/reset; inline error có “Thử lại”; toast chỉ bổ trợ |
| BL-08 | Nâng transaction table | Dễ quét và truy vết | Đủ employer, package, amount/currency, date, status; caption và `scope="col"` đúng chuẩn |
| BL-09 | Thêm pagination transaction | Điều hướng được dữ liệu lớn | Hiển thị tổng bản ghi, trang hiện tại, page size 20; first/prev/next/last hoặc tối thiểu prev/next đúng contract |
| BL-10 | Scoped loading/empty/error/retry | Widget hoạt động độc lập | Summary, tiers và transactions có state riêng; request lỗi không xóa dữ liệu đã tải |

### P1 — Khả năng đọc và an toàn thao tác

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| BL-11 | Chuẩn hóa tier card | So sánh các gói nhanh | Hiển thị name, badge, price/currency, usage progress, features và popular state |
| BL-12 | Chuẩn hóa transaction status badge | Nhận biết trạng thái không chỉ qua màu | Badge có icon/dot + label; failed dùng error semantic, pending dùng warning, paid/success dùng success |
| BL-13 | Format thời gian theo timezone | Tránh nhầm ngày giao dịch | Date dùng locale/timezone cài đặt; tooltip chứa timestamp tuyệt đối nếu API cung cấp |
| BL-14 | Đồng bộ filter/page vào URL | Giữ trạng thái khi refresh/chia sẻ | Query params chứa `status`, `page`, `pageSize`; đổi filter reset về page 1 |
| BL-15 | Thêm unsaved-change guard cho drawer | Tránh mất thay đổi | Close/Escape/navigation cảnh báo khi form dirty; focus quay lại nút mở drawer |
| BL-16 | Cải thiện phản hồi mutation | Xác nhận đối tượng vừa đổi | Success message chứa tier name; error được map thành thông báo có hành động, không lộ technical detail |

### P2 — Cần API hoặc quyết định nghiệp vụ mới

| ID | Nhiệm vụ | Dependency | Không được làm trước contract |
|---|---|---|---|
| BL-17 | Date range dùng chung | Summary/transactions phải nhận `from`, `to`, timezone | Không render control disabled như thể hoạt động |
| BL-18 | Revenue trend | Endpoint time-series current/previous | Không dựng số từ MRR hiện tại |
| BL-19 | Subscription mix | Endpoint breakdown theo tier với count/percentage | Không suy diễn từ `usagePct` của tier |
| BL-20 | Tier versioning/proration | Effective date, version, impact preview, audit | Không hứa “áp dụng ngay” khi backend chưa xác nhận |
| BL-21 | Transaction detail/invoice/refund | Detail, invoice URL, refund permission/reason/audit | Không thêm action menu giả |

## 5. Contract dữ liệu và ranh giới capability

### API hiện có

| Khu vực | Endpoint | Dữ liệu UI sử dụng |
|---|---|---|
| Summary | `GET /api/admin/billing/summary` | `monthlyRecurringRevenue`, `mrrGrowthPct`, `activeSubscriptions` |
| Tiers | `GET /api/admin/billing/tiers` | name, badge, priceMonthly, currency, usagePct, features, isPopular |
| Update tier | `PATCH /api/admin/billing/tiers/{id}` | priceMonthly, features; response updated/updatedAt nếu có |
| Transactions | `GET /api/admin/billing/transactions` | items + pagination; status filter hiện có |

### Chưa có contract

- Date range cho summary/transactions.
- Revenue time series và comparison period.
- Subscription mix aggregate.
- Transaction detail, invoice và refund.
- Tier effective date, versioning và proration.

Mỗi capability mới cần DTO typed, permission, validation errors theo field, audit metadata và controller test trước khi chuyển sang Available.

## 6. Mô hình component đề xuất

```text
BillingComponent
├── AdminPageHeader
├── BillingSummary
│   └── AdminMetricCard × 3
├── BillingTierGrid
│   └── BillingTierCard × N
├── BillingTierDrawer
│   └── Typed edit form
└── BillingTransactionSection
    ├── AdminFilterBar
    ├── AdminDataTable
    │   └── AdminStatusBadge
    └── AdminPagination
```

Giữ component/service trong bounded context `features/admin`. Chỉ đưa component sang shared khi đã có ít nhất hai trang dùng chung cùng contract.

## 7. Typed UI state đề xuất

```ts
type BillingLoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';

interface BillingPageState {
  summaryState: BillingLoadState;
  tiersState: BillingLoadState;
  transactionsState: BillingLoadState;
  selectedTierId: string | null;
  updatingTierId: string | null;
}

interface TierEditFormValue {
  priceMonthly: number;
  features: string[];
}
```

Không dùng `any` cho summary, selected tier hoặc form payload. Dùng model trong `admin-api.models.ts` làm nguồn sự thật.

## 8. Quy tắc form chỉnh sửa tier

- Drawer có accessible title chứa tên tier.
- Price là numeric input có min theo contract; không silently return khi invalid.
- Features nên dùng chip/list editor; nếu tạm dùng chuỗi phân cách dấu phẩy phải trim, bỏ item rỗng và hiển thị preview trước khi lưu.
- Submit label là “Lưu gói”, có spinner và disable request trùng.
- Update thành công: đóng drawer, refresh tier bị ảnh hưởng và đưa focus về trigger.
- Update thất bại: giữ nguyên input, hiển thị inline error và cho retry.
- Rời drawer khi dirty: xác nhận discard; `Escape` tuân theo cùng rule.
- Không cho chỉnh `currency`, `popular`, version hoặc effective date nếu API không hỗ trợ.

## 9. Token và quy tắc visual

| Thành phần | Token/giá trị bắt buộc |
|---|---|
| Canvas | `ad-surface` = `#F8FAFC` |
| Card/table/drawer | `ad-surface-container-lowest` = `#FFFFFF`; border `ad-outline-variant` = `#DCE5F2` |
| Filter/input/header | `ad-surface-container-low` = `#F1F5F9` |
| CTA/focus/link | `ad-primary` = `#2563EB`; hover `ad-primary-dim` = `#1D4ED8` |
| Popular/accent | `ad-tertiary` = `#A855F7`; không dùng làm CTA thường |
| Status | `ad-success`, `ad-warning`, `ad-error`; luôn kèm text/icon |
| Typography | Heading Manrope; body Inter; title 30/38 semibold; section 20/28; table 13/20 |
| Geometry | input/button 8px; card/table 12px; drawer 16px; pill 9999px |

Card mặc định dùng border, không dùng shadow. Shadow chỉ dành cho drawer/sticky element. Không thêm hex tùy ý khi semantic token đã tồn tại.

## 10. State và interaction contract

| State | Summary | Tiers | Transactions |
|---|---|---|---|
| Loading lần đầu | 3 metric skeleton | tier-card skeleton | row skeleton giữ header |
| Reloading | Giữ số cũ | Giữ cards và disable tier đang update | Giữ table/filter/page |
| Empty | Không áp dụng nếu API hợp lệ | “Chưa có gói dịch vụ” | Phân biệt chưa có giao dịch và không có kết quả lọc |
| Error | Inline error + retry | Inline error + retry | Inline error + retry, giữ status/page |
| Success mutation | Refresh summary nếu giá ảnh hưởng MRR theo contract | Cập nhật tier và toast chứa tên | Không áp dụng khi chưa có mutation |
| Permission denied | Forbidden page/action ẩn | Không render edit nếu thiếu `billing.update` | Read-only với `billing.read` |

## 11. Accessibility và responsive acceptance

### Accessibility

- Mọi input có label; lỗi inline nối bằng `aria-describedby` và `aria-invalid`.
- Drawer trap focus, đóng bằng `Escape` khi không dirty và trả focus về trigger.
- Transaction table có caption ẩn, `scope="col"`; status có accessible label.
- Usage progress có text percentage, không chỉ thanh màu.
- Failed transaction có icon + text “Thất bại”.
- Focus ring thống nhất `2px #2563EB`, offset `2px`.
- Live region thông báo kết quả lưu và tải lại.

### Responsive

| Breakpoint | Acceptance |
|---|---|
| 1440px | 3 KPI; tier cards 3 cột; table không gây scroll ngang toàn trang |
| 1024px | Padding 24px; tier cards 2–3 cột tùy nội dung; table giữ action chính |
| 768px | Tier cards 1–2 cột; drawer full-screen; filter transaction không tràn |
| 390px | Padding 16px; KPI/tier 1 cột; CTA ≥44px; table scroll trong container |
| 200% zoom | Không mất edit action, filter, pagination hoặc drawer footer |

## 12. Test plan

### Component tests

- Render đúng ba KPI và format số.
- Tier cards hiển thị popular, usage, currency và features đúng model.
- Click “Chỉnh sửa gói” mở đúng tier; cancel không gọi API.
- Invalid price/features không submit và hiển thị inline error.
- Update thành công đóng drawer; lỗi giữ form và cho retry.
- Status filter reset page 1 và gọi query đúng.
- Pagination giữ status filter và disable ở trang đầu/cuối.
- Loading, empty và error state của ba vùng hoạt động độc lập.

### Integration/a11y tests

- Route `/admin/billing` yêu cầu `billing.read` theo capability khi RBAC được triển khai.
- Action edit chỉ hiển thị với `billing.update` khi permission contract tồn tại.
- Keyboard-only flow: filter → table → pagination → edit drawer.
- Axe/WCAG cho default, drawer open, empty và error states.
- Responsive visual check tại 1440, 1024, 768 và 390px.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false
```

## 13. Checklist review sau mỗi lần nâng UI

- [ ] Chỉ dùng endpoint/field có trong §5; không có mock production.
- [ ] Copy tiếng Việt có dấu và action dùng động từ cụ thể.
- [ ] Mọi số tiền dùng locale + currency formatter.
- [ ] Tier card chỉ đọc; mutation nằm trong drawer typed form.
- [ ] Form update lỗi giữ nguyên dữ liệu và có retry.
- [ ] Summary, tiers, transactions có loading/empty/error độc lập.
- [ ] Filter và pagination giữ query state đúng.
- [ ] Transaction table có caption, scope và semantic status.
- [ ] Không hiển thị invoice/refund/chart/date range khi thiếu contract.
- [ ] Token màu, typography, radius và spacing đúng §9.
- [ ] Keyboard, focus return, Escape và unsaved-change guard hoạt động.
- [ ] Kiểm tra 1440/1024/768/390px và zoom 200%.
- [ ] Chạy build/test và lưu evidence trong PR.

## 14. Definition of done cho Billing

Billing được xem là hoàn thành cho capability hiện tại khi toàn bộ P0 và P1 đạt, test plan ở §12 chạy thành công, checklist §13 được xác nhận và không có capability P2 nào bị mô tả như đang hoạt động. Revenue trend, subscription mix, date range, invoice và refund chỉ chuyển sang Available sau khi API, permission, audit và error contract được triển khai và kiểm thử.
