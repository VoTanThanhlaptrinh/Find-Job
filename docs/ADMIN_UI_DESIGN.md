# Đặc tả UI Admin Portal — Job Listing

> Phiên bản: 1.0 · Phạm vi: thiết kế implementation-ready · Theme: light · Ưu tiên desktop, responsive đầy đủ

## 1. Mục tiêu và nguyên tắc

Admin Portal là không gian vận hành riêng cho đội ngũ Job Listing. Thiết kế mới phải giúp quản trị viên đọc tình trạng hệ thống nhanh, xử lý hàng đợi an toàn và truy vết được mọi thay đổi. Giao diện ưu tiên mật độ thông tin vừa phải, phân cấp rõ, ít trang trí và không dùng màu như tín hiệu duy nhất.

Ba nhãn khả năng được dùng xuyên suốt tài liệu:

- **Available**: frontend và API chính đã tồn tại, có thể cải tiến UI mà không đổi contract cốt lõi.
- **Partial**: mới có một phần UI/API; cần bổ sung contract hoặc luồng trước khi hoàn chỉnh.
- **Proposed**: năng lực roadmap; chưa được mô tả như chức năng đang hoạt động.

### Hiện trạng

Frontend admin nằm tại `job-frontend/src/app/features/admin/`. `DashboardComponent` là shell, còn `overview`, `employers`, `job-seekers`, `jobs-management` và `billing` là các trang con. Login là route độc lập.

| Khu vực | Trạng thái | Hạn chế chính |
|---|---|---|
| Login, Overview, Employers, Job Seekers, Jobs, Billing | Available | Ngôn ngữ lẫn Việt/Anh, loading dạng text, thiếu error/permission state, responsive shell chưa hoàn chỉnh |
| Categories | Partial | Có public list và admin create/update/delete nhưng chưa có trang admin |
| Admin users | Partial | Có CRUD user tổng quát, chưa có UI, RBAC hoặc audit |
| Applications, moderation, reports, support, audit, notifications, health, flags, settings | Proposed | Cần API quản trị chuyên biệt |

Các form tạo Job/Job Seeker hiện chiếm toàn bộ chiều ngang; employer detail hiển thị cuối trang; action nguy hiểm dùng `window.prompt`; biểu đồ đang biểu diễn bằng text/table. Thiết kế mới chuyển các luồng này sang dialog/drawer, có xác nhận, trạng thái và phản hồi rõ ràng.

## 2. Information architecture

| Nhóm điều hướng | Trang | Route | Khả năng |
|---|---|---|---|
| Overview | Tổng quan | `/admin/dashboard` | Available |
| User Operations | Employers | `/admin/employers` | Available |
|  | Job Seekers | `/admin/job-seekers` | Available |
|  | Applications | `/admin/applications` | Proposed |
| Recruitment | Jobs | `/admin/jobs` | Available |
|  | Categories | `/admin/categories` | Partial |
| Content & Support | Content Moderation | `/admin/content` | Proposed |
|  | Support Cases | `/admin/support` | Proposed |
| Revenue | Billing | `/admin/billing` | Available |
|  | Reports | `/admin/reports` | Proposed |
| Administration | Admin Users & RBAC | `/admin/admin-users` | Partial |
|  | Audit Logs | `/admin/audit-logs` | Proposed |
|  | Notifications | `/admin/notifications` | Proposed |
| Platform | System Health | `/admin/system-health` | Proposed |
|  | Feature Flags | `/admin/feature-flags` | Proposed |
|  | Settings | `/admin/settings` | Proposed |

`/admin/login` đứng ngoài shell. Sidebar nhóm mục bằng heading nhỏ; nhóm không có quyền xem bị ẩn thay vì disabled. Mỗi route có `title`, `breadcrumb`, `requiredPermission` và trạng thái badge tùy chọn (ví dụ số job chờ duyệt).

### Mô hình quyền đề xuất

Các vai trò roadmap gồm `SUPER_ADMIN`, `OPERATIONS_ADMIN`, `MODERATOR`, `FINANCE_ADMIN`, `SUPPORT_ADMIN`, `ANALYST`, `VIEWER`. Permission dùng dạng `resource.action`, ví dụ `jobs.approve`, `billing.update`, `audit.read`. Backend hiện chủ yếu kiểm tra vai trò admin; RBAC chi tiết là **Proposed**.

## 3. Design system

### Màu và token

Giữ namespace `ad-*` để hạn chế thay đổi template, nhưng chuẩn hóa giá trị theo `DESIGN.md`. Các surface xanh tím hiện có chỉ dùng cho selected/focus ở độ nhấn thấp.

| Token | Giá trị chuẩn | Cách dùng |
|---|---:|---|
| `ad-primary` | `#2563EB` | CTA chính, active navigation, link |
| `ad-primary-dim` | `#1D4ED8` | Hover/pressed của primary |
| `ad-secondary` | `#0EA5E9` | Thông tin, dữ liệu phụ, chart series 2 |
| `ad-tertiary` | `#A855F7` | Premium, AI, chart series 3; không dùng cho CTA thường |
| `ad-surface` | `#F8FAFC` | Canvas toàn trang |
| `ad-surface-container-lowest` | `#FFFFFF` | Card, table, dialog |
| `ad-surface-container-low` | `#F1F5F9` | Filter, input, table header |
| `ad-surface-selected` | `#EEF2FF` | Row/nav được chọn; kế thừa sắc xanh tím cũ |
| `ad-on-surface` | `#0F172A` | Nội dung chính |
| `ad-on-surface-variant` | `#526077` | Nội dung phụ |
| `ad-outline-variant` | `#DCE5F2` | Border |
| `ad-success` | `#15803D` | Thành công/active |
| `ad-warning` | `#B45309` | Pending/cảnh báo |
| `ad-error` | `#B91C1C` | Failed/destructive |

Text và icon phải đạt WCAG AA. Badge luôn có label hoặc icon kèm `aria-label`, không chỉ thay đổi màu. Focus ring thống nhất `2px #2563EB` với offset `2px`.

### Typography, geometry và spacing

- Heading dùng Manrope: page title 30/38 semibold, section title 20/28 semibold, card title 16/24 semibold.
- Body/label dùng Inter: body 14/22, dense table 13/20, caption 12/18. Số KPI dùng tabular numerals.
- Grid cơ sở 4px; khoảng cách phổ biến 8, 12, 16, 24, 32px. Content tối đa 1440px.
- Radius: input/button 8px, card/table 12px, dialog/drawer 16px, pill 9999px.
- Shadow chỉ dành cho overlay và sticky element; card mặc định dùng border. Motion 150–200ms và tôn trọng `prefers-reduced-motion`.
- Dùng Material Symbols hoặc một icon set thống nhất; icon hành động luôn có tooltip và accessible name.

## 4. Application shell

```text
Desktop ≥ 1280
┌──────── Sidebar 256 ────────┬──────── Top bar: breadcrumb · search · alerts · profile ───────┐
│ Job Listing Admin           ├──────────────────────────────────────────────────────────────────┤
│ grouped navigation          │ Page header + page actions                                      │
│                              │ Metrics / filters / workspace                                   │
│ profile + sign out          │                                                                  │
└──────────────────────────────┴──────────────────────────────────────────────────────────────────┘
```

- **1440px:** sidebar 256px, content padding 32px, KPI tối đa 4–5 cột.
- **1024px:** sidebar thu gọn 72px; label hiện qua tooltip; content padding 24px; chart 2 cột.
- **768px:** sidebar thành modal drawer; top bar có menu; filter nâng cao mở trong sheet; table được ưu tiên hơn card nếu vẫn đọc được.
- **390px:** content padding 16px; KPI 1 cột hoặc horizontal snap; table chuyển thành row card chỉ khi horizontal scroll làm mất hành động chính; primary action là full-width hoặc FAB có label.

Top bar lấy title từ route, không hard-code “Overview”. Search toàn cục mở command palette; khi chưa có API phải hiển thị disabled kèm “Coming later”, không giả lập kết quả. Notification và profile là menu có keyboard navigation. Mobile drawer khóa scroll nền, đóng bằng `Escape` và trả focus về nút menu.

## 5. Component dùng chung

| Component | Đặc tả |
|---|---|
| `AdminPageHeader` | Breadcrumb, title, mô tả ngắn, tối đa một primary action và hai secondary actions |
| `AdminMetricCard` | Label, value, trend, comparison period, icon; skeleton giữ nguyên kích thước |
| `AdminFilterBar` | Search debounce 300ms, filter chips, “Clear all”, advanced filter sheet; đồng bộ query params |
| `AdminDataTable` | Sort, selection, sticky header, row action menu, column visibility, page size 10/20/50; caption và scope đúng chuẩn |
| `AdminStatusBadge` | Mapping label + icon + semantic color; unknown status dùng neutral |
| `AdminChartCard` | Title, range, legend, tooltip, text summary và data-table fallback |
| `AdminDrawer` | Detail/edit nhanh; 480px desktop, full-screen mobile; unsaved-change guard |
| `AdminDialog` | Create/confirm; destructive action yêu cầu lý do khi nghiệp vụ cần và nêu hậu quả cụ thể |
| `AdminToast` | Success/error không thay thế inline validation; lỗi dài có action “Thử lại” |
| `AdminPagination` | Tổng bản ghi, trang hiện tại, page size, first/prev/next/last; giữ filter khi chuyển trang |
| State components | Skeleton, empty state có CTA, scoped error có retry, forbidden state có đường quay lại |

### State contract áp dụng cho mọi trang

1. **Loading:** skeleton theo đúng cấu trúc, disable action gây request trùng.
2. **Empty:** phân biệt “chưa có dữ liệu” và “không có kết quả lọc”.
3. **Error:** giữ filter/input, thông báo phạm vi lỗi và cho retry tại chỗ.
4. **Success:** cập nhật optimistic chỉ khi có rollback; toast nêu đối tượng vừa thay đổi.
5. **Permission denied:** HTTP 403 hiển thị trang forbidden; action không có quyền không được render.
6. **Session expired:** lưu URL hiện tại, chuyển login, quay lại sau khi xác thực thành công.

## 6. Đặc tả từng trang

### 6.1 Login

**Route:** `/admin/login` · **Khả năng:** Available · **Quyền:** public

**Mục tiêu:** đăng nhập nhanh, rõ bối cảnh quản trị và không làm lộ thông tin xác thực. Desktop dùng split layout 5/7: panel thương hiệu Job Listing bên trái, form bên phải; mobile chỉ giữ logo, form và hỗ trợ.

```text
[Brand statement + abstract operations graphic] [Welcome back]
                                                [Email]
                                                [Password + show/hide]
                                                [Sign in]
```

- Trường: email/username, password; hỗ trợ password manager và submit bằng Enter.
- Loading nằm trong button; lỗi credential đặt trên form, validation đặt dưới field. Không cho biết tài khoản nào tồn tại.
- “Forgot password” và “Contact support” chỉ active khi có route/API; hiện đánh dấu **Proposed**.
- API hiện có: `POST /api/auth/admin/login`, `POST /api/auth/admin/logout`.
- Accessibility: focus vào heading khi mở; error summary có `role="alert"`; password toggle có `aria-pressed`.

### 6.2 Overview

**Route:** `/admin/dashboard` · **Khả năng:** Available · **Quyền:** `dashboard.read`

```text
[Header][Range 7d/30d/90d][Refresh]
[Employers][Job seekers][Pending jobs][Revenue]
[Revenue trend 2/3 width] [Job distribution 1/3]
[Pending review queue]   [System alerts / activity]
```

- KPI giữ dữ liệu hiện có: total employers, total job seekers, pending jobs, total revenue và growth.
- Revenue dùng line chart current/previous; distribution dùng donut và legend có số tuyệt đối nếu API cung cấp. Luôn có table/text fallback.
- Pending jobs hiển thị title, company, submitted date, status và quick action “Review”; trang hóa nếu vượt 10.
- System alerts và recent activity là **Proposed**; không render dữ liệu giả trong production.
- API: `GET /api/admin/dashboard/summary`, `/revenue-trend?range=30d`, `/job-distribution`, `/admin/jobs/pending`.
- Nếu một widget lỗi, các widget khác vẫn hoạt động; refresh chỉ gọi lại vùng lỗi hoặc toàn trang theo action tương ứng.

### 6.3 Employers

**Route:** `/admin/employers` · **Khả năng:** Available · **Quyền:** `employers.read`; action cần `employers.update`

```text
[Header][Export CSV]
[4 metric cards]
[Search][KYC][Account][Industry][Clear]
[Employer table................................][detail drawer]
[Pagination]
```

- Metrics: total, growth, KYC verified, pending KYC, suspended.
- Cột mặc định: employer, industry, registration date, active jobs, KYC, account, actions. Cho sort các cột có backend hỗ trợ; phần sort hiện là **Partial**.
- Click tên mở drawer gồm contact, trạng thái, activity summary và action. KYC document/review history là **Proposed**.
- Suspend mở confirmation dialog, yêu cầu lý do; restore xác nhận nhẹ. Disable row đang cập nhật và thông báo kết quả.
- Export tôn trọng filter hiện tại. Endpoint export hiện chỉ nhận `format`; filter-aware export là **Partial**.
- API: `GET /api/admin/employers/metrics`, list, detail `/{id}`, `PATCH /{id}/status`, `GET /export`.
- Mobile dùng cards với name/status/actions; filter mở bottom sheet; drawer thành full-screen.

### 6.4 Job Seekers

**Route:** `/admin/job-seekers` · **Khả năng:** Available · **Quyền:** `job_seekers.read`; tạo mới cần `job_seekers.create`

```text
[Header][Add job seeker]
[Total][Growth][Active 7d][Placed][Retention]
[Search][Resume status][Profession][Region]
[Candidate table 2/3] [Region visualization 1/3]
```

- Chuyển form tạo mới sang dialog: full name, email, profession, resume URL; giữ validation hiện có và báo lỗi URL rõ ràng.
- Cột: candidate, email, profession, resume status, last active. Click row mở detail drawer; resume preview, application history và account action là **Proposed**.
- Region distribution dùng horizontal bars, kèm danh sách code/count để screen reader đọc được.
- API: `GET /api/admin/job-seekers/metrics`, list, `POST /api/admin/job-seekers`, `GET /region-distribution`.
- Empty-filter state giữ nút clear filters; success tạo mới đóng dialog, refresh list và focus vào row mới nếu có ID thật.

### 6.5 Applications

**Route:** `/admin/applications` · **Khả năng:** Proposed · **Quyền:** `applications.read`

```text
[Header][Date range][Export]
[Submitted][Reviewing][Interview][Hired][Rejected]
[Search][Stage][Job][Employer][Risk flag]
[Application table.............................][timeline drawer]
```

- Cột: candidate, job, employer, applied at, current stage, age in stage, risk flag, last update.
- Drawer gồm candidate summary, job snapshot, resume link, immutable stage timeline và notes. Admin chỉ override stage khi có `applications.override`, nhập lý do và xác nhận.
- Cần API: funnel metrics, paginated list/filter, detail/timeline, stage override, export và audit linkage.
- Mobile ưu tiên candidate/job/stage; các cột phụ nằm trong expandable details. Lỗi resume không làm mất phần timeline.

### 6.6 Jobs

**Route:** `/admin/jobs` · **Khả năng:** Available · **Quyền:** `jobs.read`; mutation cần `jobs.update`

```text
[Header][Create job]
[Live][Pending][Applicants][Avg time to hire]
[Search][Category][Status][Expiry]
[Bulk action bar when selected]
[Jobs table + row actions][Pagination]
```

- Form tạo job chuyển sang drawer hai phần: basic information và description/schedule. Trường hiện có: title, companyId, category, description, location, expiryDate.
- Cột: selection, title, company, location, category, applications/new today, status, expiry, actions.
- Thay select + Save trong từng row bằng action menu và dialog đổi trạng thái. Bulk activate/suspend/close phải nêu số item, yêu cầu lý do cho suspend/close và báo processed/failed.
- API: `GET /api/admin/jobs/metrics`, list, `POST /api/admin/jobs`, `PATCH /{id}/status`, `POST /bulk-action`.
- Category filter phải lấy từ category service thay vì hard-code. Sau phân trang, selection chỉ giữ nếu sản phẩm chủ động hỗ trợ cross-page selection; mặc định xóa selection.

### 6.7 Categories

**Route:** `/admin/categories` · **Khả năng:** Partial · **Quyền:** `categories.manage`

```text
[Header][Add category]
[Search][Status]
[Category][Slug][Job count][Updated][Status][Actions]
```

- List dùng `GET /api/v1/categories`; create/update/delete dùng `/api/admin/categories`.
- Create/edit dialog tối thiểu cần name và các field thực tế của `CategoryRequest`; tài liệu triển khai phải lấy DTO làm nguồn sự thật.
- Delete là destructive: hiển thị số job đang dùng. Backend cần trả usage count và chặn/xử lý reassignment; đây là **Partial**.
- Empty state có “Create first category”. Mobile giữ table nếu còn đọc được; action vào overflow menu.

### 6.8 Content Moderation

**Route:** `/admin/content` · **Khả năng:** Proposed · **Quyền:** `content.moderate`

```text
[Header][Queue count]
[Tabs: Blogs | Comments | Reports]
[Search][State][Reason][Date]
[Moderation queue..............................][preview drawer]
```

- Queue columns: content/author, type, report count, detected reason, published date, moderation state.
- Drawer render nội dung đã sanitize, report evidence và author history. Action: approve, hide, reject, delete; action tiêu cực bắt buộc reason.
- Blog API hiện có phục vụ tác giả/public, chưa phải moderation contract. Cần API queue, reports, decision, reason taxonomy và history.
- Nội dung HTML/Markdown không bao giờ render raw; link ngoài mở an toàn. Preview lỗi vẫn cho phép xem metadata và xử lý report.

### 6.9 Support Cases

**Route:** `/admin/support` · **Khả năng:** Proposed · **Quyền:** `support.manage`

```text
[Header][SLA summary]
[Queue tabs][Assignee][Priority][Status]
[Case list 5/12][Conversation 7/12]
```

- Case list: requester, subject, product area, priority, SLA timer, assignee, status. Detail gồm conversation, attachments, internal notes và event history.
- Action: assign, change priority/status, reply, add note, escalate. Internal note phải khác biệt rõ với reply gửi khách hàng.
- Cần API case CRUD, thread, attachment scan, assignment, SLA và canned responses.
- Mobile là master/detail routes hoặc full-screen drawer; draft reply được giữ khi request khác lỗi.

### 6.10 Billing

**Route:** `/admin/billing` · **Khả năng:** Available · **Quyền:** `billing.read`; chỉnh tier cần `billing.update`

```text
[Header][Date range]
[MRR][Growth][Subscriptions]
[Revenue trend][Subscription mix]
[Tier table/cards][Edit drawer]
[Transaction filters + table]
```

- Tiers hiển thị name, badge, monthly price/currency, usage, features, popular state. Chỉnh giá/features trong drawer; nêu rõ thời điểm áp dụng. Versioning/proration là **Proposed**.
- Transaction columns: employer, package, amount/currency, date, status. Filter status hiện có; date range, detail, invoice và refund là **Proposed**.
- API: `GET /api/admin/billing/summary`, `/tiers`, `PATCH /tiers/{id}`, `/transactions`.
- Tiền tệ dùng `Intl.NumberFormat`, không ghép chuỗi thủ công. Failed transaction có icon + text; update tier lỗi giữ nguyên form.

### 6.11 Reports

**Route:** `/admin/reports` · **Khả năng:** Proposed · **Quyền:** `reports.read`

```text
[Header][Date range][Compare][Export]
[Tabs: Growth | Recruitment | Revenue | Retention]
[KPI row]
[Primary chart][Breakdown chart]
[Detailed data table]
```

- Bộ lọc chung: preset 7/30/90 ngày, custom range, comparison period và timezone.
- Growth: employers/candidates/jobs; Recruitment: funnel/time-to-hire; Revenue: MRR/plan/churn; Retention: active cohorts.
- Mọi chart có legend, tooltip, text summary, downloadable CSV và accessible table. Không dùng quá bốn series cùng lúc.
- Cần API aggregate theo range/dimension, export job bất đồng bộ và trạng thái file.

### 6.12 Admin Users & RBAC

**Route:** `/admin/admin-users` · **Khả năng:** Partial · **Quyền:** `admins.manage`

```text
[Header][Invite admin]
[Admin users | Roles & permissions tabs]
[Search][Role][Status]
[Users table] / [Permission matrix]
```

- User table: name/email, role, active/locked/enabled, MFA, last login, modified date, actions.
- Existing `/api/users/page`, create, update và delete cung cấp CRUD tổng quát với fullName, email, role, profile và account flags. Detail hiện trả user hiện hành, chưa phải admin-by-id.
- UI mới ưu tiên invitation thay vì đặt password thủ công. Invite, MFA/session revoke, admin detail và permission matrix là **Proposed**.
- Không cho admin tự xóa hoặc hạ quyền tài khoản cuối cùng có `SUPER_ADMIN`. Mọi thay đổi quyền yêu cầu re-authentication và được audit.

### 6.13 Audit Logs

**Route:** `/admin/audit-logs` · **Khả năng:** Proposed · **Quyền:** `audit.read`

```text
[Header][Export]
[Actor][Action][Resource][Outcome][Date/IP]
[Immutable audit table........................][event drawer]
```

- Cột: timestamp, actor, action, resource/type/id, outcome, IP, correlation ID. Drawer hiển thị before/after diff đã mask secret.
- Log chỉ đọc; không có delete/edit UI. Filter được encode vào URL để chia sẻ điều tra.
- Cần API immutable log, detail, export và retention metadata. Export lớn chạy async.
- Screen reader nhận diff theo từng field; color diff luôn kèm “Added/Removed/Changed”.

### 6.14 Notifications

**Route:** `/admin/notifications` · **Khả năng:** Proposed · **Quyền:** `notifications.manage`

```text
[Header][Unread count]
[Inbox | Rules | Templates tabs]
[Notification list][detail]
```

- Inbox hỗ trợ unread/read, severity, source và deep link. Rules định nghĩa event, audience, channel, throttle và enabled state. Templates có subject/body, locale và preview.
- SSE hiện có thể cung cấp transport thời gian thực nhưng chưa có admin inbox/rules contract.
- Cần API list/read state, rule CRUD, template CRUD/versioning và test delivery. Template test phải dùng allow-list recipient.
- Không hiển thị bell dot nếu count chưa tải; lỗi realtime chuyển polling và thông báo trạng thái kết nối kín đáo.

### 6.15 System Health

**Route:** `/admin/system-health` · **Khả năng:** Proposed · **Quyền:** `platform.read`

```text
[Header][Environment][Auto refresh]
[API][Database][Redis][RabbitMQ][Storage][Email]
[Latency/errors charts]
[Queue/backlog table][Recent incidents]
```

- Service tile: status, latency, last checked và dependency note. Trạng thái gồm operational, degraded, outage, unknown.
- Dữ liệu nhạy cảm như hostname, credential, stack trace không xuất hiện cho role chỉ đọc.
- Cần health aggregate bảo vệ bằng quyền, time-series metrics, queue depth và incident timeline. Có thể tận dụng Spring Actuator nhưng phải whitelist field.
- Auto refresh mặc định 30 giây, pause khi tab ẩn; lỗi health endpoint hiển thị unknown, không mặc định coi là outage.

### 6.16 Feature Flags

**Route:** `/admin/feature-flags` · **Khả năng:** Proposed · **Quyền:** `flags.manage`

```text
[Header][Create flag]
[Environment selector][Search][State][Owner]
[Flag key][Description][Rollout][Environment][Updated][Toggle]
```

- Drawer gồm immutable key, description, owner, environments, enabled, rollout percentage, targeting rules và expiry.
- Toggle production mở confirmation với impact summary; thay đổi rollout phải audit và hỗ trợ rollback.
- Cần provider/storage, evaluation contract, CRUD, validation, history và environment separation.
- Không dùng toggle đơn giản cho hành động có độ trễ; UI hiển thị pending cho tới khi server xác nhận.

### 6.17 Settings

**Route:** `/admin/settings` · **Khả năng:** Proposed · **Quyền:** `settings.manage`

```text
[Settings navigation]
[General | Localization | Security | Integrations | Maintenance]
[Section form..................................][Save bar]
```

- General: product name, support contact, default timezone. Localization: locale/date/currency. Security: session duration, password/MFA policy. Integrations: email, OAuth, storage, AI, webhook status. Maintenance: read-only/maintenance banner.
- Secret field chỉ cho replace/revoke, không bao giờ trả hoặc hiển thị giá trị hiện tại. Connection test có timeout và kết quả đã mask.
- Form theo từng section, có dirty indicator, reset và sticky save bar; rời trang khi chưa lưu phải xác nhận.
- Cần typed settings API, validation, version/concurrency control, secret vault integration và audit.

## 7. API hiện có và khoảng trống

| Capability hiện có | Contract |
|---|---|
| Admin authentication | `POST /api/auth/admin/login`, `POST /api/auth/admin/logout` |
| Dashboard | `GET /api/admin/dashboard/summary`, `revenue-trend`, `job-distribution`, `GET /api/admin/jobs/pending` |
| Employers | metrics, paginated list, detail, status patch, export dưới `/api/admin/employers` |
| Job seekers | metrics, paginated list, create, region distribution dưới `/api/admin/job-seekers` |
| Jobs | metrics, list, create, status patch, bulk action dưới `/api/admin/jobs` |
| Categories | public list `/api/v1/categories`; admin create/update/delete `/api/admin/categories` |
| Billing | summary, tiers/update tier, transactions dưới `/api/admin/billing` |
| Users | create, paginated list, current detail, update, delete dưới `/api/users` |

Trước khi triển khai trang **Proposed**, mỗi capability phải có: DTO typed, pagination thống nhất 1-based, filter/sort contract, permission, validation errors theo field, audit metadata và test controller. Không tạo mock production hoặc suy diễn field không có trong contract.

## 8. Quy tắc tương tác và nội dung

- Dùng tiếng Việt nhất quán cho UI; thuật ngữ nghiệp vụ có glossary. Không dùng chuỗi không dấu như “Dang tai”.
- Primary action dùng động từ cụ thể: “Tạo công việc”, “Duyệt nội dung”, “Lưu gói”; tránh “OK”.
- Search chạy sau debounce hoặc Enter; filter và pagination được phản ánh trên URL.
- Destructive action nêu đối tượng, hậu quả và khả năng hoàn tác. Suspend/reject/override yêu cầu reason; bulk action hiển thị processed/failed.
- Ngày giờ hiển thị theo timezone cài đặt, có absolute timestamp trong tooltip; tiền tệ dùng locale formatter.
- Toast không chứa dữ liệu nhạy cảm. Error technical được map thành thông báo hành động được; correlation ID có nút copy.

## 9. Accessibility và responsive acceptance

- Thứ tự tab theo thị giác; mọi control dùng được bằng bàn phím; `Escape` đóng overlay không có thay đổi chưa lưu.
- Dialog trap focus, có accessible title/description và trả focus về trigger. Live region thông báo kết quả async.
- Table có caption ẩn, `scope="col"`, sort state và selection label chứa tên row.
- Chart không phải nguồn thông tin duy nhất; cung cấp summary và bảng dữ liệu.
- Ở 1440px không có horizontal scroll toàn trang; 1024px shell thu gọn; 768px navigation thành drawer; 390px mọi action cốt lõi vẫn tiếp cận được và touch target tối thiểu 44px.
- Zoom 200% không mất nội dung hoặc action. Contrast text thường tối thiểu 4.5:1, text lớn/UI component tối thiểu 3:1.

## 10. Definition of done cho giai đoạn triển khai

- Route có title, breadcrumb, permission guard và navigation active state đúng.
- Component dùng token, không thêm hex tùy ý nếu đã có semantic token.
- Mọi request có loading, empty, error, retry và success behavior; 401/403 được xử lý thống nhất.
- Form có typed model, inline validation, submit state và unsaved-change behavior.
- Hành động mutation có unit test; bảng/filter/pagination có component test; route và guard có integration test.
- Kiểm thử responsive tại 1440, 1024, 768 và 390px; kiểm thử keyboard và axe/WCAG cho flow chính.
- Trang **Proposed** chỉ được chuyển thành Available sau khi API, permission, audit và error contract được triển khai và kiểm thử.
