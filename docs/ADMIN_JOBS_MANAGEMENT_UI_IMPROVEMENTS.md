# Kế hoạch cải thiện UI — Admin Jobs Management

> Route: `/admin/jobs`  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §3, §5, §6.6, §7–10  
> Phạm vi frontend: `job-frontend/src/app/features/admin/pages/jobs-management/`  
> Trạng thái capability: **Available**, nhưng category, metrics, create và status/bulk contract cần hoàn thiện trước khi production-ready

## 1. Mục tiêu và giá trị mang lại

Jobs Management là màn hình vận hành tin tuyển dụng. UI cần giúp quản trị viên:

1. Nắm nhanh số tin đang hoạt động, chờ duyệt, lượng ứng viên và thời gian tuyển trung bình.
2. Tìm, lọc và kiểm tra tin tuyển dụng theo công ty, danh mục, trạng thái và hạn tuyển.
3. Tạo tin trong drawer có cấu trúc, validation và dữ liệu tham chiếu thật.
4. Thay đổi trạng thái một tin an toàn, chỉ đưa ra transition hợp lệ.
5. Thực hiện bulk action có phạm vi rõ ràng và báo chính xác số thành công/thất bại.

Thiết kế không được biến dữ liệu hard-code, filter không hoạt động hoặc status backend không hỗ trợ thành capability có vẻ hoàn chỉnh.

## 2. Hiện trạng và khoảng cách cần xử lý

| Khu vực | Hiện trạng | Khoảng cách với đặc tả | Giá trị sau cải thiện |
|---|---|---|---|
| Header | Tiêu đề tiếng Anh, mô tả kỹ thuật không dấu | Chưa có primary create action đúng hierarchy | Tiêu đề “Quản lý tin tuyển dụng” và CTA rõ ràng |
| Create form | Form luôn mở, chiếm toàn chiều ngang | Mật độ cao; company/category nhập ID/text tự do; thiếu discard behavior | Drawer hai phần, lookup thật, giữ ngữ cảnh list |
| Metrics | Có 4 KPI nhưng grid khai báo 5 cột | Hierarchy lệch; growth/time-to-hire backend đang hard-code; loading bằng text | 4 card cân đối, skeleton/error và số liệu thật |
| Search/filter | Search gọi theo mỗi ký tự; category hard-code; thiếu expiry | Thiếu debounce, URL sync, clear all; category filter chưa lọc thật | Filter chính xác, refresh/chia sẻ không mất trạng thái |
| Bulk action | Luôn hiện, select action + Apply | Không nêu phạm vi/hậu quả/reason; chưa có pending; kết quả không chi tiết | Contextual bar chỉ hiện khi chọn, confirmation an toàn |
| Table | Đủ cột chính | Copy lẫn Anh/Việt; thiếu caption/scope, badge, format ngày và responsive behavior | Dễ quét, accessible, giữ action chính |
| Row action | Select status + nút Lưu trong mọi row | Cho status không hợp lệ; mật độ cao; không giải thích hậu quả | Action menu + status dialog theo transition |
| Selection | Dùng `Set` cục bộ | Có thể giữ ID qua page/filter ngoài ý muốn; trạng thái select-all có thể sai | Mặc định selection chỉ trong page và xóa khi query đổi |
| Pagination | Chỉ có trước/sau | Thiếu page size, first/last và URL state | Điều hướng dữ liệu lớn rõ ràng |
| Async/error | Lỗi chủ yếu qua toast; lỗi list xóa dữ liệu | Request lỗi có thể trông như empty; mutation bulk không có progress/error chi tiết | Scoped loading/error/retry, giữ dữ liệu cũ |
| Responsive | Table chỉ scroll ngang, form/bulk bar wrap cơ bản | 390px có thể mất status/action | Mobile row cards hoặc table ưu tiên cột chính; drawer full-screen |

## 3. Rủi ro contract cần xử lý trước UI production

| ID | Hiện trạng contract | Rủi ro | Yêu cầu |
|---|---|---|---|
| JMC-01 | Category không tham gia query; response dùng category request hoặc `"General"` | Filter có vẻ hoạt động nhưng chỉ đổi nhãn mọi row | Join category thật và filter theo ID/slug; không phản chiếu request thành dữ liệu |
| JMC-02 | `newApplicationsToday` luôn bằng 0 | Hiển thị số liệu giả | Aggregate theo timezone/kỳ đo thật hoặc tạm ẩn |
| JMC-03 | Growth luôn 12% và avg time-to-hire luôn 18 ngày | KPI không phản ánh dữ liệu thật | Aggregate thật, định nghĩa comparison window/cohort |
| JMC-04 | Create chỉ lưu title, description, expiry date | Company, category và location nhập xong nhưng bị bỏ | Lưu đầy đủ quan hệ/field hoặc thu hẹp form đúng khả năng backend |
| JMC-05 | Create trả ID cố định `job_new` | Không thể focus/link đúng tin vừa tạo | Trả ID và status thật từ entity đã lưu |
| JMC-06 | Create/status/bulk request thiếu validation theo field | Dữ liệu sai hoặc action lạ có thể thành lỗi 500 | Typed DTO + bean validation + stable error code/field errors |
| JMC-07 | UI có `expired`/`closed`; `EntityStatus` chỉ có active/pending/rejected/suspended/deleted | Row/bulk action có thể ném `IllegalArgumentException` | Xác định state machine thật; expired là derived hay enum; close có action hợp lệ trước khi hiển thị |
| JMC-08 | Status response không có `updatedAt` như frontend model | Typed contract sai lệch | Đồng bộ response DTO; không khai báo field không được trả |
| JMC-09 | Bulk loop dừng khi gặp lỗi và controller trả 400 rỗng | Không biết item nào đã đổi trước lỗi; processed/failed không đáng tin | Transaction strategy rõ; trả kết quả từng item hoặc processed/failed + error IDs |
| JMC-10 | Test bulk dùng `archive` nhưng enum không hỗ trợ, service bị mock | Test controller không chứng minh action chạy thật | Thêm integration/service test cho từng action hợp lệ và failure giữa batch |
| JMC-11 | Frontend có `sortBy/sortDir`, design có expiry filter nhưng controller chưa nhận | UI có thể tạo control không tác dụng | Chỉ bật sau server-side contract |
| JMC-12 | Category select trong UI hard-code | Danh mục lệch nguồn dữ liệu thật | Dùng `CategoryService`/`GET /api/v1/categories`; xử lý loading/error/empty |

Theo `ADMIN_UI_DESIGN.md`, không tạo mock production hoặc suy diễn field không có trong contract. JMC-01 đến JMC-10 là blocker cho vùng UI tương ứng.

## 4. Bố cục đích

### Desktop — 1440px

```text
[Breadcrumb / Quản lý tin tuyển dụng]
[Tiêu đề + mô tả]                                      [Tạo tin tuyển dụng]

[Đang hoạt động] [Chờ duyệt] [Tổng ứng viên] [Thời gian tuyển TB]

[Tìm title/company......] [Danh mục] [Trạng thái] [Hạn tuyển*] [Xóa]

[Bulk action bar: N đã chọn | action | thực hiện]  (chỉ khi selection > 0)

[ ] [Title | Company | Location | Category | Applications | Status | Expiry | ⋯]
[..............................................................................]
[Tổng bản ghi] [Page size]                         [First][Prev][Page][Next][Last]

[Create drawer 560–640px] / [Status confirmation dialog]
```

Expiry filter, sort và các status/action mới chỉ xuất hiện khi backend contract hỗ trợ.

### Responsive

- **1024px:** metrics 2 × 2; filter wrap; ẩn bớt cột phụ theo priority nhưng không mất status/action.
- **768px:** filter nâng cao mở trong sheet; create drawer full-screen; bulk bar sticky phía dưới khi có selection.
- **390px:** metrics một cột hoặc horizontal snap; list dùng row cards nếu table scroll làm mất action; bulk bar hiển thị count + một CTA mở action sheet.
- Không có horizontal scroll toàn trang. Nếu giữ table, scroll chỉ nằm trong table container.

## 5. Danh sách nhiệm vụ triển khai

### P0 — Tính đúng đắn và luồng vận hành bắt buộc

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| JM-01 | Chuẩn hóa copy sang tiếng Việt có dấu | Nhất quán Admin Portal | Không còn `Jobs Management`, `Create`, `Live postings`, `Bulk action`, `Apply`, `Dang tai`, `Khong co` trong UI |
| JM-02 | Xử lý blocker JMC-01–JMC-10 | Không hiển thị dữ liệu/action giả | Vùng phụ thuộc chỉ bật sau backend contract và test xác nhận |
| JM-03 | Thay `any` và status string tự do bằng type | Giảm lỗi runtime/transition | Metrics dùng `AdminJobsMetrics`; status/action là union/enum đồng bộ backend; error/state typed |
| JM-04 | Chuyển create form sang drawer hai phần | Giảm nhiễu và giữ ngữ cảnh | Basic information + description/schedule; 560–640px desktop, full-screen mobile |
| JM-05 | Dùng lookup thật cho company/category/location | Ngăn ID/text sai | Company autocomplete/selector theo contract; category từ CategoryService; location theo dữ liệu được hỗ trợ |
| JM-06 | Hoàn thiện create validation | Ngăn dữ liệu không hợp lệ | Required/min-max, expiry tương lai/timezone, field errors; submit disabled khi invalid/pending |
| JM-07 | Chuẩn hóa create success/error/dirty | Không mất input hoặc focus sai | Error giữ form; success bằng ID thật đóng drawer, refresh và focus row; dirty close có discard confirmation |
| JM-08 | Thêm state độc lập cho metrics/list/lookups | Lỗi một API không phá toàn trang | Initial loading, refreshing, empty, error, retry; reload giữ dữ liệu cũ |
| JM-09 | Chuẩn hóa search/filter | Giảm request thừa và giữ trạng thái | Search debounce 300ms hoặc Enter; filter/page/pageSize vào URL; đổi filter reset page 1 |
| JM-10 | Nâng table và pagination | Dễ đọc và điều hướng | Caption, `scope="col"`, tổng, page size, first/prev/next/last; status/date format đúng |
| JM-11 | Thay select + Save bằng action menu | Chỉ đưa ra transition hợp lệ | Menu theo current status/permission; action mở confirmation dialog, không đổi status trực tiếp trong row |
| JM-12 | Thiết kế status confirmation | Làm rõ đối tượng và hậu quả | Title chứa job; suspend/close cần reason theo contract; pending per-row; lỗi giữ dialog và input |
| JM-13 | Làm bulk bar theo ngữ cảnh | Giảm nhiễu và sai phạm vi | Chỉ hiện khi có selection; nêu count/page scope; confirmation nêu action và số item |
| JM-14 | Chuẩn hóa bulk result | Không báo success chung khi có item lỗi | Pending chống submit trùng; kết quả processed/failed và failed IDs/reasons; refresh đúng vùng |
| JM-15 | Reset selection khi query/page đổi | Tránh thao tác nhầm cross-page | Mặc định xóa selection sau filter/search/page/reload; chỉ giữ cross-page nếu có product contract rõ |

### P1 — Khả năng đọc, accessibility và responsive

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| JM-16 | Chuẩn hóa 4 metric cards | Quét tình trạng nhanh | Grid 4 card cân đối; tabular numerals; growth có kỳ so sánh và không mặc định dấu `+` |
| JM-17 | Dùng status badge | Không nhận biết trạng thái chỉ bằng màu | Label tiếng Việt + icon/dot + semantic token; unknown dùng neutral |
| JM-18 | Format applications và expiry | Đọc nhanh, tránh nhầm timezone | Total/new today tách hierarchy; không hiện `+0`; expiry dùng locale/timezone và overdue semantics đúng contract |
| JM-19 | Selection accessible | Screen reader hiểu phạm vi chọn | Header checkbox có mixed state; row label chứa job title; target ≥44px; bulk count live announcement |
| JM-20 | Permission-aware actions | Không đưa action trái quyền | Trang cần `jobs.read`; create/update/bulk hiển thị theo permission; backend vẫn enforce |
| JM-21 | Filter bar responsive | Không tràn ở mobile | Desktop inline; mobile sheet; active filter chips và “Xóa tất cả” |
| JM-22 | Mobile job cards | Giữ thông tin/action cốt lõi ở 390px | Title/company, status, expiry, applications và selection/action dễ tiếp cận |
| JM-23 | Keyboard/focus hoàn chỉnh | Flow dùng được không cần chuột | Menu/dialog/drawer trap/return focus; Escape theo dirty state; tab order đúng thị giác |
| JM-24 | Live feedback cho async operation | Screen reader nhận kết quả | Live region cho create, row mutation, bulk result và reload; toast không lộ dữ liệu nhạy cảm |

### P2 — Chỉ triển khai sau API/permission contract

| ID | Nhiệm vụ | Dependency | Không được làm trước contract |
|---|---|---|---|
| JM-25 | Expiry filter | `expiryFrom/expiryTo` hoặc expiry state + timezone | Không lọc riêng dữ liệu của page hiện tại |
| JM-26 | Server-side sorting | Whitelist `sortBy`, `sortDir`, stable secondary sort | Không render sortable header khi API bỏ qua tham số |
| JM-27 | Cross-page selection | Selection token/snapshot, total matching và confirmation | Không giữ ID âm thầm qua page/filter |
| JM-28 | Job detail/review drawer | Detail endpoint, review permission, audit | Không dựng detail từ list item và gọi là hồ sơ đầy đủ |
| JM-29 | Bulk retry failed-only | Per-item result/idempotency contract | Không retry cả batch gây mutation lặp |
| JM-30 | Scheduled status/effective date | State machine, scheduler, timezone và audit | Không hứa “đóng vào ngày…” nếu backend chưa hỗ trợ |

## 6. Contract dữ liệu và ranh giới capability

### API hiện có ở mức giao diện

| Khu vực | Endpoint | Field UI dự kiến dùng | Ghi chú |
|---|---|---|---|
| Metrics | `GET /api/admin/jobs/metrics` | live, growth, pending, applicants, avg time | Growth/time-to-hire đang hard-code |
| List | `GET /api/admin/jobs` | items + pagination; search/category/status | Category và new-today chưa là dữ liệu thật |
| Create | `POST /api/admin/jobs` | title, companyId, category, description, location, expiry | Backend hiện chỉ lưu ba field và trả ID giả |
| Status | `PATCH /api/admin/jobs/{id}/status` | status/reason, updated entity metadata | Status enum/validation/response đang lệch frontend |
| Bulk | `POST /api/admin/jobs/bulk-action` | jobIds, action, reason; result từng item | Hiện loop tuần tự, failure contract chưa đủ |

### Status state machine cần chốt

Trước khi làm action menu/dialog, backend và product phải xác định:

- Tập trạng thái lưu thật và trạng thái suy diễn từ `expiryDate`.
- Transition hợp lệ theo current state.
- Action nào cần reason, permission hoặc confirmation mạnh.
- “Close” là status mới, action map sang status nào, hay nghiệp vụ riêng.
- “Expired” do hệ thống tính tự động hay admin có thể đặt.
- Response mutation có status cuối cùng, updatedAt, actor/audit ID và conflict behavior hay không.

UI không tự suy ra state machine từ danh sách option hiện tại.

### Quyền

- Đọc trang/list/metrics cần `jobs.read`.
- Tạo mới cần permission create riêng nếu RBAC tách nhỏ; tối thiểu thuộc nhóm mutation được policy cho phép.
- Row status và bulk action cần `jobs.update`.
- Backend vẫn phải chặn request trực tiếp; ẩn action trong UI không thay thế authorization.

## 7. Mô hình component đề xuất

```text
JobsManagementComponent
├── AdminPageHeader
│   └── CreateJobAction
├── JobsMetrics
│   └── AdminMetricCard × 4
├── AdminFilterBar
│   ├── JobSearchField
│   ├── CategoryFilter
│   ├── StatusFilter
│   └── ActiveFilterChips
├── JobBulkActionBar (selection > 0)
├── JobsList
│   ├── AdminDataTable
│   │   ├── AdminStatusBadge
│   │   └── JobRowActionMenu
│   └── JobRowCardList (mobile)
├── AdminPagination
├── CreateJobDrawer
└── JobStatusDialog / BulkActionDialog
```

Giữ component/service trong `features/admin`. Chỉ chuyển primitive sang `shared` khi ít nhất hai trang dùng cùng một contract hành vi.

## 8. Typed UI state đề xuất

```ts
type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';
type MutationState = 'idle' | 'submitting' | 'success' | 'error';

interface JobsManagementState {
  metricsState: LoadState;
  listState: LoadState;
  categoryState: LoadState;
  createState: MutationState;
  updatingJobId: string | null;
  bulkState: MutationState;
  selectedJobIds: ReadonlySet<string>;
}

interface JobFilters {
  search?: string;
  categoryId?: string;
  status?: AdminJobStatus;
  page: number;
  pageSize: number;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

interface JobStatusChangeFormValue {
  action: AdminJobStatusAction;
  reason?: string;
}
```

Không dùng `any` cho metrics/error. Không dùng `string` tự do cho status/action khi backend đã chốt state machine.

## 9. Quy tắc create drawer

### Phần 1 — Thông tin cơ bản

- Title có min/max length và trim theo backend.
- Company là lookup theo tên nhưng submit ID thật; không yêu cầu admin nhớ `companyId`.
- Category lấy từ CategoryService, submit ID/slug đúng contract; có loading/error/empty.
- Location dùng data source/format mà job domain hỗ trợ; không nhận text rồi bỏ qua.

### Phần 2 — Mô tả và lịch

- Description có helper text, min/max length và xử lý plain/rich text theo policy.
- Expiry phải sau thời điểm hiện tại và sau publish/effective time nếu contract yêu cầu.
- `datetime-local` phải được chuyển với timezone rõ ràng; review summary hiển thị absolute time trước submit.

### Hành vi form

- Drawer title “Tạo tin tuyển dụng”; submit label “Tạo tin tuyển dụng”.
- Field error nối bằng `aria-describedby`; control lỗi có `aria-invalid="true"`.
- Pending disable submit, giữ cancel theo policy và ngăn request trùng.
- Error giữ toàn bộ input; field/global error có retry.
- Dirty drawer hỏi trước khi discard bằng close/Escape/backdrop/navigation.
- Success chỉ khi backend trả ID thật; đóng drawer, refresh metrics/list và focus row nếu nằm trong kết quả.

## 10. Quy tắc status và bulk action

### Row status action

- Action menu chỉ liệt kê transition hợp lệ cho current status và permission hiện tại.
- Menu item dùng động từ nghiệp vụ: “Kích hoạt”, “Đình chỉ”, “Đóng tin”, “Từ chối”… theo state machine thật.
- Action thay đổi mạnh mở dialog nêu job, company, hậu quả và khả năng hoàn tác.
- Suspend/close/reject yêu cầu reason nếu contract quy định; reason được giữ khi lỗi.
- Chỉ disable row đang mutation; success lấy status từ response hoặc reload, không tự đoán.

### Bulk selection/action

- Header checkbox chọn các row đủ điều kiện trên page hiện tại; hiển thị mixed state khi chọn một phần.
- Khi page/filter/search thay đổi, xóa selection mặc định và thông báo ngắn nếu cần.
- Bulk bar chỉ xuất hiện khi selection > 0, hiển thị “Đã chọn N tin” và action hợp lệ cho toàn bộ selection.
- Nếu selected rows có status khác nhau, chỉ đưa action giao nhau hoặc giải thích item nào không đủ điều kiện.
- Confirmation nêu action, count, hậu quả và reason; không dùng nút chung chung “Apply”.
- Response phải hiển thị processed/failed; khi có lỗi một phần, cho xem failed items/reasons và retry failed-only nếu API idempotent.
- Không thông báo “thành công” cho toàn batch khi `failed > 0`.

## 11. Quy tắc list, filter và pagination

- Search theo title/company, debounce 300ms hoặc Enter; trim input và ignore response cũ.
- Category options lấy từ service; không hard-code engineering/product/design/marketing.
- Query params là nguồn sự thật cho search/category/status/page/pageSize.
- Đổi filter/search/page size reset page 1 và selection.
- Empty-filter có “Xóa bộ lọc”; empty hệ thống có create CTA nếu có quyền.
- Applications hiển thị total; “mới hôm nay” chỉ hiện khi >0 và dữ liệu có định nghĩa timezone thật.
- Expiry format theo locale/timezone; trạng thái sắp hết hạn/quá hạn cần rule từ backend/product.
- Pagination có tổng bản ghi, page size, first/prev/next/last và không làm mất filter.

## 12. Token và quy tắc visual

| Thành phần | Token/giá trị bắt buộc |
|---|---|
| Canvas | `ad-surface` = `#F8FAFC` |
| Card/table/drawer/dialog | `ad-surface-container-lowest` = `#FFFFFF`; border `ad-outline-variant` |
| Filter/input/table header | `ad-surface-container-low` = `#F1F5F9` |
| CTA/focus/link | `ad-primary`; hover `ad-primary-dim` |
| Status | `ad-success`, `ad-warning`, `ad-error`; luôn kèm text/icon |
| Typography | Heading Manrope; body Inter; title 30/38 semibold; table 13/20; caption 12/18 |
| Geometry | input/button 8px; card/table 12px; drawer/dialog 16px; badge 9999px |

- Card/table mặc định dùng border, không thêm shadow tùy ý.
- KPI/applications dùng tabular numerals.
- Bulk bar dùng surface khác nhẹ và sticky khi cần, không che pagination/action.
- Unknown status dùng neutral; không tự map thành active/pending.
- Focus ring 2px, offset 2px; không thêm raw hex khi đã có semantic token.

## 13. State và phản hồi hệ thống

| State | Metrics | Jobs list | Create | Row/bulk mutation |
|---|---|---|---|---|
| Initial loading | 4 skeleton cards | Header + row skeleton | Lookup skeleton khi mở | Không áp dụng |
| Refreshing | Giữ số cũ | Giữ list/filter/page | Giữ form | Disable đúng row/bar, progress label |
| Empty | Không biến unknown thành 0 | Phân biệt empty/empty-filter | Lookup empty có hành động phù hợp | Không áp dụng |
| Error | Inline error + retry | Inline error + retry, giữ query | Giữ input, field/global error | Giữ reason/selection; retry an toàn |
| Partial failure | Không áp dụng | Giữ rows hiện tại | Không áp dụng | processed/failed + failed item detail |
| 401 | Flow session hết hạn thống nhất | Như shell | Không báo success | Không báo success |
| 403 | Forbidden page | Không render trái quyền | Ẩn create | Ẩn action; backend vẫn enforce |
| Success | Chỉ render aggregate thật | List + pagination đúng | Đóng bằng ID thật | Cập nhật/reload, live announcement |

Không biến request lỗi thành empty state. Toast chỉ bổ trợ; scoped error phải nằm gần vùng bị lỗi.

## 14. Accessibility và responsive acceptance

### Accessibility

- Trang có một `h1`; metrics/list có heading hợp lý.
- Table có caption ẩn, `scope="col"`; sort chỉ dùng `aria-sort` khi server-side sort hoạt động.
- Checkbox header có `indeterminate`; row checkbox label chứa job title.
- Status không chỉ dựa vào màu; menu trigger có accessible name chứa job title.
- Drawer/dialog trap focus, Escape theo dirty state và trả focus về trigger.
- Live region thông báo selection count, mutation result và partial failures.
- Contrast text ≥4.5:1; component/focus ≥3:1; touch target ≥44px ở mobile.

### Responsive

| Breakpoint | Acceptance |
|---|---|
| 1440px | 4 KPI một hàng; filter/table không gây scroll ngang toàn trang |
| 1024px | KPI 2 × 2; table giữ title/status/action; cột phụ có priority rõ |
| 768px | Filter sheet; create drawer full-screen; bulk bar không che nội dung |
| 390px | Padding 16px; row cards; selection/action/status/expiry luôn tiếp cận được |
| Zoom 200% | Không mất create, filter, bulk bar, pagination, dialog/drawer footer |

## 15. Test plan

### Component/service tests

- Render đúng 4 KPI; growth dương/âm/zero/unknown không bị gắn dấu sai.
- Category options đến từ CategoryService; loading/error/empty hoạt động.
- Search debounce/Enter; URL khôi phục filter/page; đổi query reset page và selection.
- Empty, empty-filter và error được phân biệt; retry giữ query.
- Status badge map mọi status/unknown đúng label, icon và token.
- Expiry format timezone đúng; applications mới không hiện khi 0/unknown.
- Header checkbox có none/mixed/all state đúng với page hiện tại.
- Page/filter change xóa selection; bulk bar chỉ hiện khi selection >0.
- Action menu chỉ hiện transition hợp lệ; thiếu permission không có mutation.
- Row mutation pending/error/success không ảnh hưởng sai row khác.
- Bulk partial failure hiển thị processed/failed, giữ failed selection và không báo full success.
- Create drawer open/close/focus/dirty behavior đúng; invalid field không submit.
- Create error giữ form; success dùng ID thật, đóng drawer và refresh đúng vùng.
- Pagination giữ filter và disable đúng ở page đầu/cuối.

### Backend/contract tests bắt buộc

- Category filter thay đổi tập kết quả và category response đến từ quan hệ thật.
- New-applications-today, growth và avg time-to-hire aggregate từ dữ liệu thật.
- Create lưu company/category/location cùng các field còn lại; trả ID/status thật.
- Invalid create/status/bulk request trả 4xx field/stable error, không 500.
- Mỗi transition hợp lệ/không hợp lệ có service + integration test.
- Status response khớp typed frontend contract.
- Bulk full success, partial failure, invalid ID và failure giữa batch trả kết quả đáng tin.
- Pagination thống nhất 1-based; invalid page/pageSize có behavior xác định.

### Integration/a11y/responsive tests

- Route yêu cầu `jobs.read`; create/update/bulk theo permission policy.
- Keyboard-only flow: create drawer → filter → selection → row menu/dialog → bulk dialog → pagination.
- Axe/WCAG cho default, loading, empty, error, drawer, row dialog và bulk partial failure.
- Visual check tại 1440, 1024, 768, 390px và zoom 200%.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false

cd ../job-backend
./mvnw test
```

## 16. Checklist review sau mỗi lần nâng UI

- [ ] Đối chiếu thay đổi với `ADMIN_UI_DESIGN.md` §6.6, §8–10.
- [ ] Không hiển thị category, new applicants, growth hoặc time-to-hire mock như dữ liệu thật.
- [ ] Copy tiếng Việt có dấu; primary/mutation action dùng động từ cụ thể.
- [ ] Create nằm trong drawer hai phần và dùng company/category/location lookup thật.
- [ ] Create lưu đủ field, lỗi giữ form, response trả ID thật và dirty-close được bảo vệ.
- [ ] Metrics/list/category lookup có loading, refreshing, empty, error và retry riêng.
- [ ] Search có debounce/Enter; filter/page đồng bộ URL và reset selection đúng.
- [ ] Category không hard-code; expiry filter/sort không xuất hiện trước contract.
- [ ] Table có caption/scope; status badge có label + icon + semantic token.
- [ ] Row không còn select + Save; action menu chỉ có transition hợp lệ.
- [ ] UI không gửi `expired/closed` khi backend chưa hỗ trợ.
- [ ] Suspend/close/reject yêu cầu reason theo contract và dialog giữ input khi lỗi.
- [ ] Bulk bar chỉ hiện khi chọn; nêu phạm vi; partial failure không báo full success.
- [ ] Selection mặc định bị xóa khi page/filter/search thay đổi.
- [ ] Permission `jobs.read/update` và create policy được áp dụng ở UI/backend.
- [ ] Token màu, typography, radius và spacing đúng §12.
- [ ] Kiểm tra keyboard, axe, 1440/1024/768/390px và zoom 200%.
- [ ] Chạy frontend build/test và backend tests liên quan; lưu evidence trong PR.

## 17. Definition of done cho Jobs Management

Trang Jobs Management được xem là hoàn thành cho capability hiện tại khi:

1. JMC-01 đến JMC-10 đã được xử lý hoặc vùng UI/action phụ thuộc được ẩn rõ ràng.
2. Toàn bộ nhiệm vụ P0 và P1 đạt acceptance criteria.
3. Create, row status và bulk flow hoạt động bằng keyboard, tuân theo state machine/permission và xử lý partial failure đúng.
4. Test plan ở §15 chạy thành công và checklist §16 được xác nhận.
5. Không capability P2 nào bị mô tả như đang hoạt động trước khi API, permission, validation, idempotency và audit contract được triển khai, kiểm thử.
