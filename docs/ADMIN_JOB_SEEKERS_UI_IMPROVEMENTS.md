# Kế hoạch cải thiện UI — Admin Job Seekers

> Route: `/admin/job-seekers`  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §3, §5, §6.4, §7–10  
> Phạm vi frontend: `job-frontend/src/app/features/admin/pages/job-seekers/`  
> Trạng thái capability: **Available**, nhưng dữ liệu metrics, profession, resume, last-active, region và create contract chưa production-ready

## 1. Mục tiêu và giá trị mang lại

Job Seekers là màn hình vận hành hồ sơ người tìm việc. UI cần giúp quản trị viên:

1. Nắm nhanh quy mô, tăng trưởng, mức độ hoạt động, số ứng viên được tuyển và tỷ lệ duy trì.
2. Tìm kiếm, lọc và đọc danh sách ứng viên với trạng thái hồ sơ rõ ràng.
3. Tạo hồ sơ mới an toàn trong dialog, có validation và không làm mất ngữ cảnh danh sách.
4. Hiểu phân bổ khu vực bằng visualization có dữ liệu thay thế cho screen reader.
5. Mở rộng sang detail, resume preview, application history và account action chỉ khi có API và permission đầy đủ.

Thiết kế không được hiển thị dữ liệu mock hoặc suy diễn như dữ liệu vận hành thật. Capability chưa có contract phải được ẩn hoặc ghi rõ dependency trong tài liệu triển khai.

## 2. Hiện trạng và khoảng cách cần xử lý

| Khu vực | Hiện trạng | Khoảng cách với đặc tả | Giá trị sau cải thiện |
|---|---|---|---|
| Header | Tiêu đề tiếng Anh, mô tả kỹ thuật không dấu | Không nhất quán ngôn ngữ; chưa có primary action đúng vị trí | Tiêu đề nghiệp vụ rõ, nút “Thêm người tìm việc” dễ thấy |
| Create form | Form luôn mở, chiếm toàn chiều ngang | Gây nhiễu; thiếu URL validation rõ, error contract và unsaved-change behavior | Form nằm trong dialog, chỉ xuất hiện khi cần |
| Metrics | Có 5 card, loading bằng text | Thiếu hierarchy, comparison context, skeleton/error; backend đang suy diễn 4 chỉ số | Quét nhanh và chỉ trình bày số liệu thật |
| Search/filter | Search gọi API theo mỗi ký tự; chỉ có resume status | Thiếu debounce, URL sync, clear filters; resume filter chưa lọc thật | Filter ổn định, refresh/chia sẻ không mất trạng thái |
| Candidate table | Có đủ 5 cột cơ bản | Copy không dấu/lẫn Anh; thiếu caption/scope, badge, sort, format thời gian | Dễ quét, dùng được với keyboard/screen reader |
| Pagination | Chỉ có trước/sau | Thiếu page size, first/last và query-param state | Điều hướng dữ liệu lớn rõ ràng |
| Region distribution | Chỉ là danh sách code/count | Chưa có horizontal bars; dữ liệu backend đang hard-code | Visualization dễ so sánh, có list/table fallback |
| Detail | Chưa có | Design yêu cầu click row mở drawer nhưng chưa có detail endpoint | Chỉ triển khai sau khi có contract, không dựng từ dữ liệu giả |
| Async/error | Lỗi chủ yếu qua toast; lỗi list xóa dữ liệu | Không có scoped retry; request lỗi có thể trông như empty | Mỗi vùng có loading/empty/error/retry độc lập |
| Responsive | Form và bảng co theo grid/scroll | 390px dễ mất cột/action; chưa có dialog/sheet acceptance | Mobile list/card và dialog full-screen dễ thao tác |

## 3. Rủi ro contract cần xử lý trước UI production

| ID | Hiện trạng contract | Rủi ro | Yêu cầu |
|---|---|---|---|
| JC-01 | `profession` trong list luôn là `"Job Seeker"` | Hiển thị nghề nghiệp giả; không thể filter chính xác | Lưu và truy vấn profession thật hoặc tạm ẩn field/filter |
| JC-02 | `resumeStatus` không tham gia điều kiện query, chỉ phản chiếu request | Filter có vẻ hoạt động nhưng không đổi tập kết quả | Có nguồn resume thật và query đúng; tạm ẩn filter nếu chưa có |
| JC-03 | `lastActiveAt` fallback sang `LocalDateTime.now()` | Ứng viên không hoạt động có thể bị hiển thị là vừa hoạt động | Trả `null`/unknown hoặc timestamp hoạt động thật; không suy diễn “bây giờ” |
| JC-04 | Growth, active 7d, placed và retention dùng hằng số/tỷ lệ | KPI không phản ánh dữ liệu thật | Aggregate từ nguồn thật và định nghĩa rõ kỳ đo |
| JC-05 | Region distribution hard-code NA/EU/APAC/LATAM/MEA | Visualization tạo nhận định sai | Aggregate vùng từ dữ liệu thật, định nghĩa code/unknown và tổng mẫu |
| JC-06 | Create chỉ lưu `fullName` và `email` | UI báo tạo thành công nhưng profession/resume URL bị bỏ | Lưu toàn bộ field hoặc thu hẹp form/contract đúng khả năng thật |
| JC-07 | Create luôn trả ID `cand_new` | Không thể focus/link đúng record vừa tạo | Trả ID thật từ entity đã lưu |
| JC-08 | Request chưa có bean validation/field-error contract | Dữ liệu sai có thể qua backend; UI khó map lỗi | Validate required/email/URL/length; trả lỗi theo field |
| JC-09 | Frontend khai báo sort nhưng controller chưa nhận | Header sortable có thể chỉ sắp xếp page hiện tại hoặc không có tác dụng | Chỉ bật sort sau server-side contract |
| JC-10 | Không có detail endpoint | Drawer/resume/history/account action không có nguồn dữ liệu | Xây API, DTO, permission và audit trước khi render capability |

Theo `ADMIN_UI_DESIGN.md`, không tạo mock production hoặc suy diễn field không có trong contract. JC-01 đến JC-08 là blocker cho các vùng UI tương ứng.

## 4. Bố cục đích

### Với contract production-ready

```text
[Breadcrumb / Người tìm việc]
[Tiêu đề + mô tả]                               [Thêm người tìm việc]

[Tổng số] [Tăng trưởng] [Hoạt động 7 ngày] [Đã tuyển] [Duy trì]

[Tìm tên/email........] [Trạng thái CV] [Nghề nghiệp*] [Khu vực*] [Xóa]

[Candidate table................................] [Phân bổ khu vực]
[Candidate | Email | Profession | Resume | Active] [Horizontal bars]
[Pagination.....................................] [Accessible list]

[Create dialog] / [Detail drawer*]
```

`Nghề nghiệp`, `Khu vực`, sort và detail drawer chỉ hoạt động sau khi có contract tương ứng. Không render control disabled như một tính năng đã sẵn sàng.

### Responsive

- **1440px:** 5 KPI trên một hàng; table 2/3 và region 1/3; không scroll ngang toàn trang.
- **1024px:** KPI 3 + 2 hoặc grid linh hoạt; table và region có thể xếp dọc nếu thiếu không gian đọc.
- **768px:** filter nâng cao mở trong sheet; create/detail là full-screen dialog/drawer.
- **390px:** KPI một cột hoặc horizontal snap; bảng chuyển row cards nếu scroll ngang làm mất thông tin chính; touch target tối thiểu 44px.

## 5. Danh sách nhiệm vụ triển khai

### P0 — Tính đúng đắn và luồng cốt lõi

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| JS-01 | Chuẩn hóa copy sang tiếng Việt có dấu | Nhất quán Admin Portal | Không còn `Job Seekers`, `Create`, `Growth`, `Placed`, `Last active`, `Dang tai`, `Khong co` trong UI |
| JS-02 | Xử lý blocker JC-01–JC-08 | Không hiển thị dữ liệu hoặc success giả | Chỉ bật vùng phụ thuộc sau khi backend contract và test xác nhận |
| JS-03 | Thay `any` bằng model typed | Giảm lỗi runtime | Metrics dùng `AdminJobSeekersMetrics`; region dùng `AdminRegionDistribution`; error/state có type rõ |
| JS-04 | Chuyển create form vào dialog | Giảm nhiễu và giữ ngữ cảnh | Header có “Thêm người tìm việc”; dialog 560–640px desktop/full-screen mobile; form không render thường trực |
| JS-05 | Hoàn thiện typed create form | Ngăn dữ liệu sai trước submit | Validate name, email, profession và URL theo contract; inline error nối bằng `aria-describedby` |
| JS-06 | Chuẩn hóa create success/error | Không mất dữ liệu hoặc focus sai | Success đóng dialog, reload metrics/list và focus row bằng ID thật; lỗi giữ form, map field errors và cho retry |
| JS-07 | Thêm unsaved-change behavior | Tránh mất nội dung đang nhập | Close/Escape/backdrop cảnh báo khi form dirty; cancel xác nhận discard; focus trở lại trigger |
| JS-08 | Thêm state độc lập theo widget | Lỗi một API không phá toàn trang | Metrics, list và region có initial loading, refreshing, empty, error, retry riêng |
| JS-09 | Chuẩn hóa search/filter | Giảm request thừa, giữ trạng thái | Search debounce 300ms hoặc Enter; filter/page/pageSize đồng bộ URL; đổi filter reset page 1 |
| JS-10 | Phân biệt empty và empty-filter | Cho bước tiếp theo đúng ngữ cảnh | Empty-filter có “Xóa bộ lọc”; empty hệ thống có mô tả và create CTA nếu có quyền |
| JS-11 | Nâng table và pagination | Dễ đọc và điều hướng | Caption ẩn, `scope="col"`, tổng bản ghi, page size, first/prev/next/last; giữ filter khi đổi trang |
| JS-12 | Region visualization accessible | So sánh vùng nhanh mà không mất accessibility | Horizontal bars theo tỷ lệ thật, label code/count rõ; list/table fallback chứa cùng dữ liệu |

### P1 — Khả năng đọc, permission và responsive

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| JS-13 | Chuẩn hóa metric cards | Đọc nhanh trạng thái nguồn ứng viên | Tabular numerals; growth có kỳ so sánh; percentage có precision thống nhất; unknown không thành 0 |
| JS-14 | Dùng status badge cho resume | Không nhận biết chỉ bằng màu | Complete/incomplete/missing map label tiếng Việt + icon/dot + semantic token; unknown dùng neutral |
| JS-15 | Format last-active đúng timezone | Tránh hiểu sai thời gian hoạt động | Relative label chỉ bổ trợ; tooltip có absolute timestamp; `null` hiển thị “Chưa có dữ liệu” |
| JS-16 | Permission-aware create | Không đưa action trái quyền | Trang/list cần `job_seekers.read`; create chỉ hiện với `job_seekers.create`; backend vẫn enforce |
| JS-17 | Filter bar responsive | Giữ list dễ đọc ở màn hình nhỏ | Desktop inline; mobile bottom sheet; filter chips và “Xóa tất cả” |
| JS-18 | Mobile candidate cards | Không mất field chính ở 390px | Card có name/email, profession nếu thật, resume badge, last-active; thứ tự đọc hợp lý |
| JS-19 | Keyboard/focus hoàn chỉnh | Flow dùng được không cần chuột | Tab order theo thị giác; dialog trap/return focus; filter sheet và pagination có accessible names |
| JS-20 | Live feedback cho async state | Screen reader nhận kết quả | Live region thông báo create/reload/filter error; toast không lộ dữ liệu nhạy cảm |

### P2 — Chỉ triển khai sau API/permission contract

| ID | Nhiệm vụ | Dependency | Không được làm trước contract |
|---|---|---|---|
| JS-21 | Profession filter | Profession thật, danh mục/normalization và query param | Không lọc riêng dữ liệu của page hiện tại |
| JS-22 | Region filter | Candidate region thật, code mapping và query param | Không dùng distribution hard-code làm danh mục |
| JS-23 | Server-side sorting | Whitelist `sortBy`, `sortDir`, stable secondary sort | Không render header sortable khi API bỏ qua tham số |
| JS-24 | Candidate detail drawer | Detail endpoint, typed DTO, `job_seekers.read` | Không dựng detail từ list item và gọi đó là hồ sơ đầy đủ |
| JS-25 | Resume preview/download | Secure file URL, content type, expiry, malware/access policy | Không iframe/link trực tiếp URL chưa xác thực |
| JS-26 | Application history | Paginated history endpoint và permission | Không suy diễn lịch sử từ metrics |
| JS-27 | Account action | Status mutation, reason, permission, audit và confirmation | Không thêm suspend/activate button giả |
| JS-28 | Deep-link drawer | Route/query convention và guard | Không ghi ID vào URL trước khi có detail resolution/error behavior |

## 6. Contract dữ liệu và ranh giới capability

### API hiện có ở mức giao diện

| Khu vực | Endpoint | Field UI dự kiến dùng | Ghi chú |
|---|---|---|---|
| Metrics | `GET /api/admin/job-seekers/metrics` | total, growth, active 7d, placed, retention | Bốn field đang suy diễn; cần aggregate thật |
| List | `GET /api/admin/job-seekers` | items + pagination; search/resume status | Resume filter và một số field đang mock/fallback |
| Create | `POST /api/admin/job-seekers` | fullName, email, profession, resumeUrl; ID trả về | Backend hiện bỏ profession/resume URL và trả ID giả |
| Region | `GET /api/admin/job-seekers/region-distribution` | regions code/count | Dữ liệu hiện hard-code |

### Chưa có contract

- Candidate detail.
- Resume preview/download metadata.
- Application history.
- Account status/action.
- Profession và region filter.
- Server-side sort.

Mỗi capability mới cần DTO typed, pagination phù hợp, permission, validation errors theo field, audit metadata và controller/integration test trước khi chuyển sang Available.

## 7. Mô hình component đề xuất

```text
JobSeekersComponent
├── AdminPageHeader
│   └── CreateJobSeekerAction
├── JobSeekerMetrics
│   └── AdminMetricCard × 5
├── AdminFilterBar
│   ├── CandidateSearchField
│   ├── ResumeStatusFilter
│   └── ActiveFilterChips
├── JobSeekerWorkspace
│   ├── CandidateList
│   │   ├── AdminDataTable
│   │   └── CandidateRowCardList
│   └── RegionDistributionCard
│       ├── HorizontalBars
│       └── AccessibleRegionList
├── AdminPagination
├── CreateJobSeekerDialog
└── CandidateDetailDrawer (sau khi có API)
```

Giữ component/service trong `features/admin`. Chỉ đưa primitive sang `shared` khi có từ hai trang sử dụng cùng contract hành vi.

## 8. Typed UI state đề xuất

```ts
type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';

interface JobSeekersPageState {
  metricsState: LoadState;
  listState: LoadState;
  regionState: LoadState;
  createState: 'idle' | 'submitting' | 'error' | 'success';
  createDialogOpen: boolean;
  selectedCandidateId: string | null;
}

interface JobSeekerFilters {
  search?: string;
  resumeStatus?: string;
  page: number;
  pageSize: number;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

interface CreateJobSeekerFormValue {
  fullName: string;
  email: string;
  profession: string;
  resumeUrl: string;
}
```

Không dùng `any` cho metrics, region distribution, form errors hoặc page state. Resume status nên là union/enum được frontend-backend thống nhất thay vì chuỗi tự do.

## 9. Quy tắc form tạo người tìm việc

- Dialog title là “Thêm người tìm việc”; description nói rõ tài khoản/hồ sơ nào sẽ được tạo theo contract thật.
- Full name trim khoảng trắng, có min/max length từ backend.
- Email dùng validation frontend nhưng backend vẫn normalize, kiểm tra format và trùng lặp.
- Profession phải được lưu thật; nếu backend chưa hỗ trợ thì không được nhận input rồi bỏ qua.
- Resume URL phải được parse/validate scheme/domain/file policy theo contract; lỗi “URL không hợp lệ” phải tách khỏi lỗi required.
- Field error nối bằng `aria-describedby`, control lỗi có `aria-invalid="true"`.
- Submit label “Tạo người tìm việc”; có progress và chặn request trùng.
- Success chỉ báo khi backend trả record/ID thật; đóng dialog, refresh list/metrics và focus record mới nếu đang nằm trong kết quả hiện tại.
- Error giữ toàn bộ input; lỗi field hiển thị cạnh field, lỗi chung hiển thị trong dialog; cho retry.
- Form dirty áp dụng discard confirmation khi đóng, Escape hoặc điều hướng.

## 10. Quy tắc list, filter và region

### List/filter

- Search theo tên/email, có label hiển thị hoặc visually hidden; placeholder không thay label.
- Debounce 300ms hoặc Enter; trim input; hủy/ignore response cũ khi query thay đổi nhanh.
- URL query params là nguồn sự thật nếu hỗ trợ refresh/deep link.
- Resume badge dùng label “Hoàn chỉnh”, “Chưa hoàn chỉnh”, “Thiếu CV”; unknown hiển thị trung tính.
- Không bật profession/region filter hoặc sorting trước khi backend hỗ trợ.
- Last-active không bao giờ mặc định thành thời gian hiện tại khi dữ liệu thiếu.

### Region distribution

- Horizontal bar dài tương ứng `count / max(count)` hoặc percentage do API cung cấp; count vẫn luôn hiển thị bằng text.
- Sắp xếp theo rule được công bố: count giảm dần hoặc thứ tự API; không làm thay đổi âm thầm giữa reload.
- Có heading, text summary và danh sách/table fallback chứa code, label vùng và count.
- Mã unknown/null được gom thành “Chưa xác định” theo backend contract.
- Loading dùng skeleton bars; error có retry; empty không dựng các bar 0 giả.

## 11. Token và quy tắc visual

| Thành phần | Token/giá trị bắt buộc |
|---|---|
| Canvas | `ad-surface` = `#F8FAFC` |
| Card/table/dialog | `ad-surface-container-lowest` = `#FFFFFF`; border `ad-outline-variant` |
| Filter/input/table header | `ad-surface-container-low` = `#F1F5F9` |
| CTA/focus/link | `ad-primary`; hover `ad-primary-dim` |
| Status | `ad-success`, `ad-warning`, `ad-error`; luôn kèm text/icon |
| Typography | Heading Manrope; body Inter; title 30/38 semibold; table 13/20; caption 12/18 |
| Geometry | input/button 8px; card/table 12px; dialog/drawer 16px; badge 9999px |

- Card/table mặc định dùng border, không thêm shadow tùy ý.
- KPI/count dùng tabular numerals.
- Region bars dùng semantic/categorical palette có contrast; màu không thay thế label/count.
- Focus ring thống nhất 2px, offset 2px.
- Không thêm raw hex khi đã có semantic token.

## 12. State và phản hồi hệ thống

| State | Metrics | Candidate list | Region | Create dialog |
|---|---|---|---|---|
| Initial loading | 5 metric skeleton | Header + row skeleton | Bar skeleton | Không áp dụng |
| Refreshing | Giữ số cũ | Giữ list/filter/page | Giữ chart cũ | Submit progress, disable submit |
| Empty | Không biến unknown thành 0 | Phân biệt empty/empty-filter | “Chưa có dữ liệu khu vực” | Không áp dụng |
| Error | Inline error + retry | Inline error + retry, giữ query | Inline error + retry | Giữ input, field/global error, retry |
| 401 | Flow hết hạn session thống nhất | Như shell | Như shell | Không báo success |
| 403 | Forbidden page | Không render dữ liệu trái quyền | Không request trái quyền | Ẩn create action; backend vẫn enforce |
| Success | Chỉ render aggregate thật | List + pagination đúng | Bars + fallback cùng dữ liệu | Đóng, refresh, focus theo ID thật |

Không biến request lỗi thành empty state. Toast chỉ bổ trợ; scoped error phải nằm gần vùng bị lỗi.

## 13. Accessibility và responsive acceptance

### Accessibility

- Trang có một `h1`; metrics, list và region có heading hợp lý.
- Table có caption ẩn và `scope="col"`; chỉ dùng `aria-sort` khi server-side sort hoạt động.
- Resume status không chỉ dựa vào màu.
- Dialog trap focus, có title/description, Escape tuân theo dirty state và trả focus về trigger.
- Horizontal bars có text label/count và accessible list/table fallback.
- Live region thông báo create, reload và lỗi async; focus không nhảy ngoài ý muốn.
- Contrast text ≥4.5:1; component/focus ≥3:1.

### Responsive

| Breakpoint | Acceptance |
|---|---|
| 1440px | 5 KPI; table 2/3 + region 1/3; không scroll ngang toàn trang |
| 1024px | KPI wrap cân đối; table/region xếp dọc nếu cần; pagination không tràn |
| 768px | Filter sheet; create/detail full-screen; field form một cột khi cần |
| 390px | Padding 16px; KPI/card list một cột hoặc snap; target ≥44px |
| Zoom 200% | Không mất create action, filter, pagination, dialog footer hoặc region labels |

## 14. Test plan

### Component/service tests

- Render đúng 5 KPI; growth/percentage dương, âm, zero và unknown có format đúng.
- Search debounce/Enter, ignore response cũ và đổi filter reset page 1.
- URL khôi phục đúng search, resume status, page và page size.
- Empty, empty-filter và error được phân biệt; retry giữ query.
- Resume badge map complete/incomplete/missing/unknown đúng label, icon và token.
- Last-active format timezone đúng; null không biến thành “vừa xong”.
- Pagination giữ filter và disable đúng ở trang đầu/cuối.
- Region bars và accessible fallback render cùng code/count; loading/error/empty độc lập.
- Create dialog open/close/focus/dirty behavior đúng.
- Invalid name/email/profession/URL không submit; field error được nối đúng.
- Create lỗi giữ form; success dùng ID thật, đóng dialog và refresh đúng vùng.
- Thiếu `job_seekers.create` không render action tạo.

### Backend/contract tests bắt buộc

- Resume filter thực sự thay đổi tập kết quả.
- Profession, resume status và last-active đến từ dữ liệu thật; không có mock/`now()` fallback.
- Metrics và region distribution aggregate từ dữ liệu thật với kỳ đo/code định nghĩa rõ.
- Create lưu toàn bộ field contract hoặc từ chối field không hỗ trợ; response trả ID thật.
- Missing/invalid email/URL/name/profession trả field validation 4xx; email trùng có error code ổn định.
- Pagination thống nhất 1-based; invalid page/pageSize có behavior xác định.

### Integration/a11y/responsive tests

- Route yêu cầu `job_seekers.read`; create yêu cầu `job_seekers.create`.
- Keyboard-only flow: create dialog → search/filter → list → pagination → region fallback.
- Axe/WCAG cho default, loading, empty, error và create dialog.
- Visual check tại 1440, 1024, 768, 390px và zoom 200%.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false

cd ../job-backend
./mvnw test
```

## 15. Checklist review sau mỗi lần nâng UI

- [ ] Đối chiếu thay đổi với `ADMIN_UI_DESIGN.md` §6.4, §8–10.
- [ ] Không hiển thị profession, resume status, last-active, metrics hoặc region mock như dữ liệu thật.
- [ ] Copy tiếng Việt có dấu; primary action dùng động từ cụ thể.
- [ ] Create nằm trong dialog typed form, không chiếm toàn chiều ngang trang.
- [ ] Profession/resume URL được backend lưu thật; response trả ID record thật.
- [ ] Create lỗi giữ input; success đóng dialog, refresh và focus đúng khi có thể.
- [ ] Metrics/list/region có loading, refreshing, empty, error và retry riêng.
- [ ] Search có debounce/Enter; filter/page đồng bộ URL và reset page đúng.
- [ ] Table có caption/scope; resume status có label + icon + semantic token.
- [ ] Last-active thiếu dữ liệu không bị thay bằng thời điểm hiện tại.
- [ ] Region bars có text/list fallback và chỉ dùng aggregate thật.
- [ ] Profession/region filter, sort, detail/resume/history/account action không xuất hiện trước contract.
- [ ] Permission `job_seekers.read/create` được áp dụng ở UI và backend.
- [ ] Token màu, typography, radius và spacing đúng §11.
- [ ] Kiểm tra keyboard, axe, 1440/1024/768/390px và zoom 200%.
- [ ] Chạy frontend build/test và backend tests liên quan; lưu evidence trong PR.

## 16. Definition of done cho Job Seekers

Trang Job Seekers được xem là hoàn thành cho capability hiện tại khi:

1. JC-01 đến JC-08 đã được xử lý hoặc vùng UI phụ thuộc được ẩn rõ ràng.
2. Toàn bộ nhiệm vụ P0 và P1 đạt acceptance criteria.
3. Luồng tìm/lọc/phân trang, region visualization và create dialog hoạt động bằng keyboard với error handling đầy đủ.
4. Test plan ở §14 chạy thành công và checklist §15 được xác nhận.
5. Không capability P2 nào bị mô tả như đang hoạt động trước khi API, permission, validation, secure file handling và audit contract được triển khai, kiểm thử.
