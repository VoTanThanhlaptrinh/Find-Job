# Kế hoạch cải thiện UI — Admin Employers

> Route: `/admin/employers`  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §3, §5, §6.3, §7–10  
> Phạm vi frontend: `job-frontend/src/app/features/admin/pages/employers/`  
> Trạng thái capability: **Available**, nhưng một số dữ liệu và contract backend cần hoàn thiện trước khi coi là production-ready

## 1. Mục tiêu và giá trị mang lại

Employers là màn hình vận hành tài khoản nhà tuyển dụng. UI cần giúp quản trị viên:

1. Nắm nhanh quy mô, tình trạng KYC và số tài khoản bị đình chỉ.
2. Tìm, lọc và đọc danh sách nhà tuyển dụng với ngữ cảnh rõ ràng.
3. Mở chi tiết mà không mất vị trí, bộ lọc hoặc trang hiện tại.
4. Đình chỉ/khôi phục tài khoản an toàn, có lý do, xác nhận và phản hồi kết quả.
5. Xuất đúng tập dữ liệu đang xem khi backend hỗ trợ filter-aware export.

Thiết kế phải phân biệt rõ capability đã có với dữ liệu đang mock hoặc contract mới chỉ partial. Không trình bày dữ liệu giả như dữ liệu vận hành thật.

## 2. Hiện trạng và khoảng cách cần xử lý

| Khu vực | Hiện trạng | Khoảng cách với đặc tả | Giá trị sau cải thiện |
|---|---|---|---|
| Header | Tiêu đề tiếng Anh, mô tả kỹ thuật không dấu | Không nói rõ mục tiêu trang; copy không nhất quán | Tiêu đề “Nhà tuyển dụng”, mô tả ngắn theo nghiệp vụ |
| Metrics | Có bốn card và loading dạng text | Thiếu hierarchy, skeleton, error/retry; backend đang tính KYC/suspended theo tỷ lệ giả định | Quét nhanh, không hiển thị số liệu giả như fact |
| Search/filter | Search gọi API theo mỗi ký tự; KYC/account dùng select | Thiếu debounce, clear all, URL state; KYC backend chưa lọc thật; thiếu industry contract | Lọc ổn định, giữ được trạng thái khi refresh/chia sẻ |
| Table | Đủ cột chính nhưng text/status thô | Thiếu caption/scope, badge ngữ nghĩa, sort và format ngày | Dễ quét bằng mắt, keyboard và screen reader |
| Row action | Luôn hiện cả Suspend và Restore | Không phụ thuộc trạng thái/quyền; dễ chọn sai | Chỉ hiện action hợp lệ, đặt trong menu hoặc nút theo ngữ cảnh |
| Suspend | Dùng `window.prompt`, reason optional | Không có accessible dialog, validation, loading hoặc hậu quả rõ ràng | Xác nhận an toàn, bắt buộc lý do theo đặc tả |
| Restore | Frontend gửi `restore` | Backend hiện chỉ xử lý `activate`; thao tác có thể báo thành công nhưng không đổi dữ liệu | Contract thống nhất và kiểm thử end-to-end |
| Detail | Render một card ở cuối trang | Người dùng mất ngữ cảnh; thiếu loading/error/close | Drawer desktop, full-screen mobile, focus đúng |
| Pagination | Chỉ có trước/sau | Thiếu page size, first/last và URL sync | Điều hướng bộ dữ liệu lớn rõ ràng |
| Export | Nút CSV có pending state | Backend chỉ nhận `format` và trả URL mẫu | Chỉ bật khi export thật; filter-aware là dependency |
| Async/error | Lỗi chủ yếu qua toast và có thể xóa list | Không có scoped retry; khó phân biệt empty với request lỗi | Mỗi vùng có loading/empty/error/retry riêng |
| Responsive | Table chỉ scroll ngang | 390px có thể mất action chính; filter chưa có bottom sheet | Mobile cards, filter sheet, detail full-screen |

## 3. Rủi ro contract cần xử lý trước UI production

Đây là các điểm phát hiện từ contract hiện tại. Chúng không được che bằng thay đổi trình bày:

| ID | Hiện trạng contract | Rủi ro | Yêu cầu |
|---|---|---|---|
| EC-01 | `RecruitmentQuery` gán `industry = "Technology"` | Hiển thị ngành giả cho mọi employer | Lấy industry thật hoặc không render/filter field này cho đến khi có dữ liệu |
| EC-02 | `kycStatus` không tham gia điều kiện query và được phản chiếu từ request | Filter KYC có vẻ hoạt động nhưng chỉ đổi nhãn dữ liệu | Bổ sung nguồn KYC thật và query đúng; tạm ẩn KYC filter/metric nếu chưa có |
| EC-03 | Metrics KYC/suspended được suy ra bằng tỷ lệ 80/10/10 | KPI không phản ánh dữ liệu thật | Aggregate từ dữ liệu thật; scoped error khi không tải được |
| EC-04 | Export trả URL `cdn.example.com` và chỉ nhận `format` | Tải file giả; không tôn trọng filter | Tạo export thật, xác thực URL/file, nhận filter hiện tại hoặc ghi rõ export toàn bộ |
| EC-05 | Frontend dùng action `restore`, backend chỉ xử lý `suspend`/`activate` | UI có thể thông báo thành công sai | Chuẩn hóa enum dùng chung; unknown action phải trả validation error |
| EC-06 | Request status không validation; reason không bắt buộc | Mutation nguy hiểm thiếu guardrail và audit | Validate action, bắt buộc reason khi suspend, lưu audit metadata |
| EC-07 | UI khai báo `sortBy/sortDir` nhưng controller chưa nhận | Sort có thể bị dựng giả ở frontend | Chỉ bật sort sau khi backend hỗ trợ và trả contract rõ ràng |

Theo `ADMIN_UI_DESIGN.md`, không tạo mock production hoặc suy diễn field không có trong contract. EC-01 đến EC-06 là blocker cho những phần UI liên quan.

## 4. Bố cục đích

### Desktop — 1440px

```text
[Breadcrumb / Nhà tuyển dụng]
[Tiêu đề + mô tả]                                      [Xuất CSV]

[Tổng nhà tuyển dụng] [KYC đã xác minh] [KYC chờ xử lý] [Đã đình chỉ]

[Tìm tên/email................] [KYC] [Tài khoản] [Ngành*] [Xóa bộ lọc]

[Employer | Industry | Registration | Active jobs | KYC | Account | Actions]
[.........................................................................]
[Tổng bản ghi] [Page size]                       [First][Prev][Page][Next][Last]

                                             [Detail drawer 480px]
```

`Ngành`, sort và filter-aware export chỉ xuất hiện khi contract backend tương ứng đã sẵn sàng.

### Tablet và mobile

- **1024px:** metrics 2 × 2; filter có thể wrap; table ưu tiên các cột employer, KYC, account và action.
- **768px:** filter nâng cao mở trong sheet; detail drawer thành full-screen; table vẫn dùng nếu action không bị mất.
- **390px:** metrics một cột hoặc horizontal snap; list chuyển thành row cards gồm tên, ngành, hai status và action; touch target tối thiểu 44px.
- Không cho phép horizontal scroll ở toàn trang. Nếu table cần scroll, scroll chỉ nằm trong container có affordance rõ ràng.

## 5. Danh sách nhiệm vụ triển khai

### P0 — Tính đúng đắn và luồng vận hành bắt buộc

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| ER-01 | Chuẩn hóa copy sang tiếng Việt có dấu | Nhất quán Admin Portal | Không còn `Employers`, `Tim`, `Dang tai`, `Khong co`, `Suspend`, `Restore`, `Exporting` trong UI |
| ER-02 | Xử lý các blocker EC-01–EC-06 | Không hiển thị dữ liệu hoặc success giả | Capability liên quan chỉ bật sau khi backend contract và test xác nhận |
| ER-03 | Thay `any` bằng model typed | Giảm lỗi render và mutation | Metrics dùng `AdminEmployersMetrics`; detail dùng `AdminEmployerDetail`; state/error có type rõ |
| ER-04 | Thêm state độc lập cho metrics/list/detail | Lỗi một vùng không phá toàn trang | Mỗi request có initial loading, refreshing, empty, error và retry; reload giữ dữ liệu cũ |
| ER-05 | Chuẩn hóa search/filter | Giảm request thừa và giữ ngữ cảnh | Search debounce 300ms hoặc Enter; filter/page/pageSize đồng bộ URL; đổi filter reset page 1 |
| ER-06 | Phân biệt empty list và empty-filter | Đưa ra bước tiếp theo phù hợp | Empty-filter có “Xóa bộ lọc”; empty toàn hệ thống không gợi ý clear vô nghĩa |
| ER-07 | Chuyển chi tiết sang drawer | Không mất vị trí trong list | Click tên/row mở drawer 480px; có loading/error/retry/close; URL hoặc state giữ employer ID nếu sản phẩm yêu cầu deep link |
| ER-08 | Thay `window.prompt` bằng suspend dialog | Mutation an toàn và accessible | Dialog nêu employer/hậu quả; reason bắt buộc, trim và validate; trap focus; disable khi submit |
| ER-09 | Chuẩn hóa restore confirmation | Tránh thao tác nhầm | Action chỉ hiện với suspended employer; confirmation nhẹ; gửi action backend thực sự hỗ trợ |
| ER-10 | Quản lý pending theo từng row | Tránh request trùng và hiểu nhầm | Chỉ disable row đang cập nhật; spinner/label phù hợp; success cập nhật từ response hoặc reload |
| ER-11 | Nâng table và pagination | Dễ đọc và điều hướng dữ liệu lớn | Caption ẩn, `scope="col"`, tổng bản ghi, page size, first/prev/next/last; giữ filter khi đổi trang |
| ER-12 | Kiểm soát export theo contract thật | Không cung cấp file giả hoặc sai phạm vi | Ẩn/disable có giải thích khi endpoint chưa production-ready; khi bật phải tôn trọng phạm vi filter đã công bố |

### P1 — Khả năng đọc, accessibility và responsive

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| ER-13 | Chuẩn hóa metric cards | Quét tình trạng nhanh | Số dùng tabular numerals; growth có ngữ cảnh kỳ so sánh từ API; không mặc định dấu `+` cho số âm/unknown |
| ER-14 | Dùng `AdminStatusBadge` | Không nhận biết trạng thái chỉ bằng màu | KYC/account map label tiếng Việt + icon/dot + semantic color; unknown dùng neutral |
| ER-15 | Chuẩn hóa ngày đăng ký | Tránh nhầm locale/timezone | Dùng date formatter; tooltip timestamp tuyệt đối nếu API cung cấp thời gian |
| ER-16 | Action theo trạng thái và quyền | Giảm nhiễu, ngăn thao tác trái phép | Active chỉ có “Đình chỉ”; suspended chỉ có “Khôi phục”; thiếu `employers.update` thì không render mutation |
| ER-17 | Filter bar responsive | Giữ action cốt lõi trên màn hình nhỏ | Desktop inline; mobile bottom sheet; chip thể hiện filter đang áp dụng; có “Xóa tất cả” |
| ER-18 | Mobile row cards | Không mất thông tin/action tại 390px | Card có employer, industry nếu có thật, KYC, account, active jobs và action; thứ tự đọc hợp lý |
| ER-19 | Keyboard/focus hoàn chỉnh | Luồng dùng được không cần chuột | Mở/đóng overlay trả focus; Escape đúng rule; focus ring rõ; action menu có accessible name chứa employer |
| ER-20 | Live feedback cho async action | Screen reader nhận biết kết quả | Live region thông báo tải, đình chỉ, khôi phục và export; toast không chứa dữ liệu nhạy cảm |

### P2 — Chỉ triển khai sau khi có API/permission contract

| ID | Nhiệm vụ | Dependency | Không được làm trước contract |
|---|---|---|---|
| ER-21 | Industry filter | Industry field thật và query parameter backend | Không dùng giá trị hard-code hoặc lọc riêng page hiện tại |
| ER-22 | Server-side sorting | Whitelist `sortBy`, `sortDir`, stable secondary sort | Không biểu diễn header sortable khi API bỏ qua tham số |
| ER-23 | KYC document/review history | Detail API, document access, review permission và audit | Không tạo dữ liệu/timeline giả |
| ER-24 | Activity summary | Aggregate contract định nghĩa thời gian và chỉ số | Không suy diễn activity từ `activeJobs` |
| ER-25 | Filter-aware/async export | Export job, filter snapshot, permission, expiry và error state | Không dùng URL mẫu hoặc tự xuất dữ liệu chỉ của page hiện tại |
| ER-26 | Deep-link detail drawer | Route/query-param convention và guard | Không làm URL state nửa vời gây mở sai employer |

## 6. Contract dữ liệu và ranh giới capability

### API hiện có ở mức giao diện

| Khu vực | Endpoint | Field UI dự kiến dùng | Ghi chú |
|---|---|---|---|
| Metrics | `GET /api/admin/employers/metrics` | total, growth, KYC verified/pending, suspended | Cần thay số liệu suy diễn bằng aggregate thật |
| List | `GET /api/admin/employers` | items, pagination; search, KYC, account | Search/account có query; KYC cần sửa contract/data |
| Detail | `GET /api/admin/employers/{id}` | contact, industry, registration, jobs, statuses | Industry hiện chưa phải dữ liệu thật |
| Status | `PATCH /api/admin/employers/{id}/status` | action, reason | Cần enum/validation/audit và thống nhất restore/activate |
| Export | `GET /api/admin/employers/export` | format/download URL | Hiện là mock URL; filter-aware chưa có |

### Quyền và audit

- Đọc trang/list/detail/metrics cần `employers.read`.
- Đình chỉ và khôi phục cần `employers.update`.
- Export cần permission riêng nếu dữ liệu chứa thông tin liên hệ hoặc khối lượng lớn.
- Backend vẫn phải từ chối direct request khi thiếu quyền; ẩn action trong UI không thay thế authorization.
- Mutation cần lưu actor, employer ID, action, reason, timestamp và correlation/audit ID theo chính sách hệ thống.

## 7. Mô hình component đề xuất

```text
EmployersComponent
├── AdminPageHeader
│   └── EmployersExportAction
├── EmployersMetrics
│   └── AdminMetricCard × 4
├── AdminFilterBar
│   ├── SearchField
│   ├── StatusFilters
│   └── ActiveFilterChips
├── EmployersList
│   ├── AdminDataTable (desktop/tablet)
│   └── EmployerRowCardList (mobile)
├── AdminPagination
├── EmployerDetailDrawer
└── EmployerStatusDialog
```

Giữ component/service trong `features/admin`. Chỉ đưa primitive sang `shared` khi ít nhất hai trang dùng cùng một contract hành vi.

## 8. Typed UI state đề xuất

```ts
type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';

interface EmployersPageState {
  metricsState: LoadState;
  listState: LoadState;
  detailState: LoadState;
  selectedEmployerId: string | null;
  updatingEmployerId: string | null;
  exportState: 'idle' | 'exporting' | 'error';
}

interface EmployerFilters {
  search?: string;
  kycStatus?: string;
  accountStatus?: string;
  page: number;
  pageSize: number;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

interface SuspendEmployerFormValue {
  reason: string;
}
```

Không dùng `any` cho metrics, detail, error hoặc mutation state. Status từ API nên là union/enum dùng chung thay vì chuỗi tự do.

## 9. Quy tắc interaction

### Search và filter

- Search theo tên/email, có label thật; placeholder chỉ là ví dụ, không thay label.
- Debounce 300ms hoặc chỉ submit khi Enter; trim input; hủy/ignore response cũ khi query đổi nhanh.
- Query params là nguồn sự thật cho filter/page nếu trang hỗ trợ link chia sẻ.
- Đổi filter/search/page size đưa page về 1.
- “Xóa tất cả” chỉ reset filter, không làm mất cài đặt page size nếu product không yêu cầu.

### Detail drawer

- Accessible title chứa tên employer; có close button với tên rõ ràng.
- Loading dùng skeleton theo cấu trúc; lỗi detail không đóng drawer và có “Thử lại”.
- Hiển thị đúng field contract: liên hệ, ngày đăng ký, ngành nếu có thật, active jobs, KYC và account.
- KYC history/document và activity summary phải gắn nhãn dependency cho đến khi có API.
- Desktop rộng 480px; mobile full-screen; trap focus và trả focus về row/name trigger.

### Suspend và restore

- Suspend dialog nêu rõ tên employer và hậu quả truy cập; reason là textarea bắt buộc.
- Nút submit là “Đình chỉ nhà tuyển dụng”, dùng semantic error; cancel là secondary.
- Restore dùng confirmation nhẹ nhưng vẫn nêu đúng đối tượng.
- Pending disable action của đúng row/dialog; không khóa toàn bộ bảng nếu không cần.
- Lỗi giữ dialog và reason, map thành thông báo hành động được, cho retry.
- Success đóng dialog, cập nhật dữ liệu từ response/reload, thông báo qua live region và giữ focus hợp lý.

### Export

- Tên action “Xuất CSV”; pending “Đang chuẩn bị tệp…”.
- Export phải công bố phạm vi: toàn bộ hay theo filter hiện tại.
- URL tải phải đến từ endpoint thật, có expiry/error contract; không dùng domain mẫu.
- Nếu export chạy nền, cần trạng thái job và thông báo khi sẵn sàng; không giả vờ tải tức thì.

## 10. Token và quy tắc visual

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
- KPI và số lượng dùng tabular numerals.
- Status unknown dùng neutral; không tự map unknown thành active/verified.
- Focus ring thống nhất 2px, offset 2px và đủ contrast.
- Không thêm raw hex nếu semantic token đã có.

## 11. State và phản hồi hệ thống

| State | Metrics | List | Detail drawer | Mutation/export |
|---|---|---|---|---|
| Initial loading | 4 card skeleton | Header + row skeleton | Drawer skeleton | Nút disabled + progress label |
| Refreshing | Giữ số cũ, báo refresh nhẹ | Giữ list/filter/page | Giữ detail hiện tại nếu cùng ID | Disable đúng action đang chạy |
| Empty | Chỉ dùng khi contract cho phép | Phân biệt empty toàn bộ/empty-filter | Không áp dụng | Không áp dụng |
| Error | Inline error + retry | Inline error + retry, giữ query | Inline error + retry, không tự đóng | Giữ input; message có hành động |
| 401 | Chuyển flow session hết hạn thống nhất | Như shell | Đóng overlay theo session flow | Không báo success |
| 403 | Forbidden page hoặc ẩn vùng theo permission | Không render dữ liệu trái quyền | Không request detail trái quyền | Ẩn action; backend vẫn enforce |
| Success | Render số thật | Render list + pagination | Render detail thật | Refresh/cập nhật state và live announcement |

Không biến request lỗi thành danh sách rỗng. Toast chỉ bổ trợ; scoped error phải nằm gần vùng bị lỗi.

## 12. Accessibility và responsive acceptance

### Accessibility

- Page có một `h1`; section metrics/list có heading phù hợp.
- Mọi input/select có label; filter sheet/dialog có accessible title và description.
- Table có caption ẩn, `scope="col"`; sort dùng `aria-sort` chỉ khi server-side sort hoạt động.
- Status không chỉ dựa vào màu; action accessible name chứa tên employer.
- Drawer/dialog trap focus, hỗ trợ Escape theo trạng thái form và trả focus về trigger.
- Kết quả async được thông báo qua live region; loading không làm focus nhảy bất ngờ.
- Contrast text thường ≥4.5:1; UI component/focus ≥3:1.

### Responsive

| Breakpoint | Acceptance |
|---|---|
| 1440px | 4 KPI một hàng; filter/table/drawer không tạo scroll ngang toàn trang |
| 1024px | Metrics 2 × 2; filter wrap hợp lý; action chính vẫn nhìn thấy |
| 768px | Filter dùng sheet khi cần; detail full-screen; list giữ thông tin/status quan trọng |
| 390px | Padding 16px; KPI một cột/snap; row cards; action và close target ≥44px |
| Zoom 200% | Không mất filter, action, pagination, dialog footer hoặc drawer close |

## 13. Test plan

### Component/service tests

- Render metrics đúng model; growth dương, âm, zero và unknown không bị gắn dấu sai.
- Search debounce/Enter và bỏ qua response cũ; đổi filter reset page 1.
- URL khôi phục đúng search, KYC, account, page và page size.
- Empty toàn bộ, empty-filter và error được phân biệt; retry giữ query.
- Badge map verified/pending/rejected, active/suspended và unknown đúng label/icon/token.
- Click employer mở đúng detail; loading/error/retry/close hoạt động; focus được trả về.
- Active row chỉ có suspend; suspended row chỉ có restore; thiếu permission không có action.
- Suspend thiếu reason không submit; lỗi giữ reason; success cập nhật đúng row.
- Restore gửi action backend hỗ trợ và chỉ báo success khi mutation thành công.
- Pagination giữ filter và disable đúng tại trang đầu/cuối.
- Export gửi đúng phạm vi contract, ngăn click lặp và xử lý URL/error/expiry.

### Backend/contract tests bắt buộc

- KYC filter thực sự thay đổi tập kết quả, không phản chiếu request thành status.
- Industry và metrics đến từ dữ liệu thật, không có hằng số/tỷ lệ mock.
- Unknown status action trả 4xx; suspend thiếu reason trả validation error.
- Suspend/activate thay đổi record thật và response phản ánh status mới.
- Export không trả domain mẫu; file/phạm vi filter đúng và được authorization.
- Pagination thống nhất 1-based; invalid page/pageSize có behavior xác định.

### Integration/a11y/responsive tests

- Route yêu cầu `employers.read`; mutation yêu cầu `employers.update`.
- Keyboard-only flow: search → filters → table/cards → pagination → drawer/dialog.
- Axe/WCAG cho default, loading, empty, error, drawer và suspend dialog.
- Visual check tại 1440, 1024, 768, 390px và zoom 200%.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false

cd ../job-backend
./mvnw test
```

## 14. Checklist review sau mỗi lần nâng UI

- [ ] Đối chiếu thay đổi với `ADMIN_UI_DESIGN.md` §6.3, §8–10.
- [ ] Không hiển thị industry, KYC, metrics hoặc export mock như dữ liệu thật.
- [ ] Copy tiếng Việt có dấu; action dùng động từ cụ thể.
- [ ] Metrics/list/detail có loading, refreshing, empty, error và retry phù hợp.
- [ ] Search có debounce/Enter; filter/page đồng bộ URL và reset page đúng.
- [ ] Table có caption, scope; status có label + icon + semantic token.
- [ ] Action theo đúng account status và permission; direct API vẫn được bảo vệ.
- [ ] Suspend dùng dialog, yêu cầu reason; không còn `window.prompt`.
- [ ] Restore/activate thống nhất frontend-backend và có contract test.
- [ ] Row đang mutation được disable; lỗi giữ dữ liệu; không báo success giả.
- [ ] Detail dùng drawer/full-screen mobile, có focus trap/return và retry.
- [ ] Export chỉ bật với endpoint thật và phạm vi được mô tả chính xác.
- [ ] Sort, industry filter, KYC history và activity không xuất hiện trước contract.
- [ ] Token màu, typography, radius và spacing đúng §10.
- [ ] Kiểm tra keyboard, axe, 1440/1024/768/390px và zoom 200%.
- [ ] Chạy frontend build/test và backend tests liên quan; lưu evidence trong PR.

## 15. Definition of done cho Employers

Trang Employers được xem là hoàn thành cho capability hiện tại khi:

1. EC-01 đến EC-06 đã được xử lý hoặc vùng UI phụ thuộc được ẩn rõ ràng.
2. Toàn bộ nhiệm vụ P0 và P1 đạt acceptance criteria.
3. Luồng list → detail → suspend/restore hoạt động bằng keyboard, có permission và error handling đầy đủ.
4. Test plan ở §13 chạy thành công và checklist §14 được xác nhận.
5. Không có capability P2 nào bị mô tả như đang hoạt động trước khi API, permission, validation và audit contract được triển khai, kiểm thử.
