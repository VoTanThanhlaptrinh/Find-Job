# Kế hoạch cải thiện UI — Admin Dashboard Shell

> Component: `DashboardComponent`  
> Phạm vi: application shell bao quanh các route admin; không phải nội dung trang Overview  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §1–5, §8–10 và `DESIGN.md`  
> File hiện tại: `job-frontend/src/app/features/admin/pages/dashboard/`

## 1. Vai trò của Dashboard Shell

Dashboard shell chịu trách nhiệm tạo khung vận hành nhất quán cho toàn bộ Admin Portal:

- Điều hướng tới đúng capability người dùng được phép truy cập.
- Hiển thị breadcrumb và title theo route hiện tại.
- Duy trì sidebar/top bar responsive mà không che nội dung trang con.
- Cung cấp điểm vào cho search, notification, profile và sign out khi contract tương ứng tồn tại.
- Xử lý session expired, permission denied và focus navigation nhất quán.

`/admin/login` phải đứng ngoài shell. `OverviewComponent`, Employers, Job Seekers, Jobs và Billing được render trong `router-outlet` của shell.

## 2. Hiện trạng và khoảng cách cần xử lý

| Khu vực | Hiện trạng | Khoảng cách với đặc tả | Giá trị sau cải thiện |
|---|---|---|---|
| Sidebar | Cố định 256px ở mọi kích thước | Không có compact state 72px hoặc mobile drawer | Nội dung sử dụng tốt từ desktop đến mobile |
| Navigation | Danh sách phẳng, tiếng Anh | Thiếu group heading, capability metadata và permission visibility | Người dùng hiểu cấu trúc portal, chỉ thấy mục có quyền |
| Active state | Dùng raw hex, pill/shadow tùy ý | Không theo token/radius chuẩn; có nguy cơ match route sai | Active state nhất quán và dễ nhận biết |
| Top bar | Title hard-code `Overview` | Sai title khi chuyển route; thiếu breadcrumb | Giữ đúng ngữ cảnh ở mọi trang |
| Global search | Chưa có | Chưa có API/command contract | Chỉ hiển thị disabled “Sắp có” hoặc ẩn; không giả lập kết quả |
| Notification | Bell và unread dot luôn hiển thị | Dữ liệu giả, không có unread API | Không tạo tín hiệu cảnh báo sai |
| Profile | `adminName`, `adminRole`, `Last Login: 2m ago` là placeholder | Hiển thị dữ liệu không thật, không có keyboard menu | Chỉ render dữ liệu profile xác thực từ session/API |
| Logout | Button tiếng Anh; điều hướng thủ công | Thiếu pending/error behavior và session cleanup evidence | Đăng xuất rõ trạng thái, quay login an toàn |
| Content canvas | Margin/padding desktop cố định, `overflow-hidden` | Có thể cắt nội dung, không thích ứng shell state | Layout page con không bị che hoặc mất scroll |
| Accessibility | Icon set lẫn Flaticon; button bell tên `btn` | Thiếu accessible name, tooltip, focus flow, drawer semantics | Shell dùng được hoàn toàn bằng keyboard/screen reader |
| Route config | Chỉ có path/component và generic admin guard | Thiếu title, breadcrumb, requiredPermission và badge metadata | Shell render UI từ route source of truth |

## 3. Bố cục đích

### Desktop ≥1280px

```text
┌──────── Sidebar 256px ────────┬──── Top bar: breadcrumb · search · alerts · profile ────┐
│ Job Listing Admin             ├───────────────────────────────────────────────────────────┤
│                               │                                                           │
│ TỔNG QUAN                     │  router-outlet                                            │
│   Tổng quan                   │  page header + page workspace                             │
│ VẬN HÀNH NGƯỜI DÙNG          │                                                           │
│   Nhà tuyển dụng              │                                                           │
│   Người tìm việc              │                                                           │
│ TUYỂN DỤNG                    │                                                           │
│   Công việc                   │                                                           │
│ DOANH THU                     │                                                           │
│   Gói dịch vụ                 │                                                           │
│                               │                                                           │
│ Profile · Đăng xuất           │                                                           │
└───────────────────────────────┴───────────────────────────────────────────────────────────┘
```

### Responsive states

| Viewport | Shell behavior |
|---|---|
| 1440px | Sidebar 256px; content padding page 32px; đầy đủ label/group/profile |
| 1024px | Sidebar 72px; ẩn label; tooltip cho icon; content padding page 24px |
| 768px | Sidebar thành modal drawer; top bar có menu button; backdrop khóa scroll nền |
| 390px | Top bar gọn; breadcrumb truncate hợp lý; action ≥44px; content padding page 16px |

## 4. Information architecture và capability visibility

### Navigation có thể hiển thị ngay

| Group | Label | Route | Capability |
|---|---|---|---|
| Tổng quan | Tổng quan | `/admin/dashboard` | Available |
| Vận hành người dùng | Nhà tuyển dụng | `/admin/employers` | Available |
| Vận hành người dùng | Người tìm việc | `/admin/job-seekers` | Available |
| Tuyển dụng | Công việc | `/admin/jobs` | Available |
| Doanh thu | Gói dịch vụ | `/admin/billing` | Available |

### Navigation chưa được mô tả như hoạt động

- Categories và Admin Users là **Partial**: chỉ thêm route/menu sau khi UI, permission và contract cần thiết hoàn tất.
- Applications, moderation, reports, support, audit, notifications, health, flags và settings là **Proposed**: không thêm menu active hoặc màn hình giả.
- Mục không có quyền phải bị ẩn, không render disabled.
- Nếu permission model chi tiết chưa có, tiếp tục dùng admin role guard hiện tại và ghi rõ RBAC là dependency; không suy diễn quyền ở frontend.

## 5. Route metadata làm nguồn sự thật

Mỗi child route cần khai báo metadata thay vì hard-code trong shell:

```ts
interface AdminRouteData {
  title: string;
  breadcrumb: string;
  requiredPermission?: string;
  navigationGroup: 'overview' | 'user-operations' | 'recruitment' | 'revenue';
  navigationIcon: string;
  badgeKey?: string;
}
```

Ví dụ mục tiêu:

```ts
{
  path: 'billing',
  component: BillingComponent,
  data: {
    title: 'Doanh thu và gói dịch vụ',
    breadcrumb: 'Gói dịch vụ',
    requiredPermission: 'billing.read',
    navigationGroup: 'revenue',
    navigationIcon: 'credit_card',
  },
}
```

Cho đến khi RBAC backend sẵn sàng, `requiredPermission` có thể là metadata chuẩn bị trước nhưng không được dùng để giả lập quyền.

## 6. Danh sách nhiệm vụ triển khai

### P0 — Shell usable và không hiển thị dữ liệu giả

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| DS-01 | Đổi branding/copy sang Job Listing và tiếng Việt | Nhất quán sản phẩm | Không còn `EliteHire`, `Dashboard`, `Employers`, `Logout` trong shell |
| DS-02 | Xóa placeholder profile/last login | Không gây hiểu nhầm dữ liệu | Không render `adminName`, `adminRole`, `2m ago`; chỉ dùng profile thật |
| DS-03 | Xóa unread dot giả | Không báo động sai | Bell/unread chỉ xuất hiện khi có notification contract; chưa có thì ẩn hoặc disabled “Sắp có” |
| DS-04 | Dùng title/breadcrumb từ route | Đúng ngữ cảnh khi điều hướng | Chuyển route cập nhật top bar; không hard-code `Overview` |
| DS-05 | Nhóm navigation theo IA | Dễ quét và mở rộng | Có heading nhỏ cho Overview, User Operations, Recruitment, Revenue |
| DS-06 | Chuẩn hóa active/hover/focus state | Nhất quán visual và keyboard | Dùng semantic token; active state có text/icon, không chỉ màu |
| DS-07 | Bảo đảm canvas không cắt nội dung | Tránh mất scroll/action | Không dùng `overflow-hidden` cho main nếu page con cần scroll; offset đúng sidebar/top bar |
| DS-08 | Chuẩn hóa logout flow | Đăng xuất an toàn | Disable khi pending; clear session; success về login; lỗi có thông báo hành động được |

### P1 — Responsive và accessibility

| ID | Nhiệm vụ | Giá trị UI/UX | Điều kiện hoàn thành |
|---|---|---|---|
| DS-09 | Sidebar 256px/72px theo breakpoint | Tối ưu không gian desktop/tablet | ≥1280 đầy đủ label; 1024 compact; icon có tooltip/accessibility name |
| DS-10 | Mobile navigation drawer | Dùng được tại 768/390px | Menu button có `aria-expanded`; drawer có backdrop, lock scroll, Escape, focus trap/return |
| DS-11 | Dùng một icon set | Giảm nhiễu và lỗi tải asset | Material Symbols hoặc icon set thống nhất; icon decorative có `aria-hidden` |
| DS-12 | Thêm skip link | Keyboard user đi thẳng nội dung | “Bỏ qua điều hướng” focus-visible, target main content hợp lệ |
| DS-13 | Chuẩn hóa landmark | Screen reader hiểu cấu trúc | Dùng `header`, `nav`, `aside`, `main`; không lồng/nhân đôi `main` sai semantics |
| DS-14 | Tôn trọng reduced motion | Tránh khó chịu/chóng mặt | Transition 150–200ms; `prefers-reduced-motion` tắt animation không cần thiết |
| DS-15 | Profile menu keyboard-ready | Truy cập account/logout dễ dàng | Trigger có name/expanded; arrow/tab/Escape hợp lý; click outside; focus return |

### P2 — Phụ thuộc capability mới

| ID | Nhiệm vụ | Dependency | Quy tắc |
|---|---|---|---|
| DS-16 | Command palette search | Search contract và permission-aware results | Khi chưa có API chỉ disabled “Sắp có”; không giả lập kết quả |
| DS-17 | Notification menu/badge | Inbox/unread API hoặc SSE contract | Không hiển thị dot khi count chưa tải; lỗi realtime không mặc định thành unread |
| DS-18 | Permission-aware navigation | RBAC permission list từ session/API | Mục thiếu quyền bị ẩn; direct route trả forbidden |
| DS-19 | Dynamic badge pending jobs | Typed count source và error behavior | Không hiển thị count stale/giả; unknown state không hiện badge |
| DS-20 | Admin profile/session menu | Typed profile, last login, MFA/session APIs | Không suy diễn initials/role/time từ placeholder |

## 7. Component architecture đề xuất

```text
DashboardComponent (or AdminShellComponent)
├── AdminSkipLink
├── AdminSidebar
│   ├── AdminBrand
│   ├── AdminNavigationGroup × N
│   │   └── AdminNavigationItem × N
│   └── AdminAccountSummary
├── AdminTopBar
│   ├── MobileMenuButton
│   ├── AdminBreadcrumb
│   ├── GlobalSearchTrigger
│   ├── NotificationTrigger
│   └── AdminProfileMenu
└── main#admin-main
    └── router-outlet
```

Không cần tách component chỉ để giảm số dòng. Tách khi một phần có state/interaction độc lập, có test riêng hoặc được tái sử dụng.

## 8. Typed navigation model đề xuất

```ts
interface AdminNavigationItem {
  label: string;
  route: string;
  icon: string;
  capability: 'available' | 'partial' | 'proposed';
  requiredPermission?: string;
  badge?: number | null;
}

interface AdminNavigationGroup {
  id: string;
  label: string;
  items: AdminNavigationItem[];
}

interface AdminShellState {
  mobileNavigationOpen: boolean;
  profileMenuOpen: boolean;
  compactSidebar: boolean;
  navigationItems: AdminNavigationGroup[];
}
```

Navigation label/route/icon không nên được lặp lại độc lập trong template và route config. Chọn một typed config hoặc route metadata làm nguồn sự thật.

## 9. Token và visual rules

| Thành phần | Token/giá trị bắt buộc |
|---|---|
| App canvas | `ad-surface` = `#F8FAFC` |
| Sidebar/top bar | `ad-surface-container-lowest` = `#FFFFFF` |
| Border | `ad-outline-variant` = `#DCE5F2` |
| Active navigation | `ad-surface-selected` = `#EEF2FF`, text/icon `ad-primary` |
| Primary/focus | `ad-primary` = `#2563EB`; hover `ad-primary-dim` = `#1D4ED8` |
| Main text | `ad-on-surface` = `#0F172A` |
| Secondary text | `ad-on-surface-variant` = `#526077` |
| Error/sign out warning | `ad-error` = `#B91C1C` khi semantic cần thiết |

- Brand/top-bar title dùng Manrope; navigation/body dùng Inter.
- Nav item/button radius 8px; selected state không dùng pill quá tròn nếu làm giảm mật độ.
- Sidebar/top bar sticky/fixed có thể dùng shadow nhẹ; navigation card mặc định không dùng shadow.
- Không dùng raw hex như `#282b51`, `#d9daff`, `#7b9cff` hoặc `#ffffff` trong template.
- Touch target tối thiểu 44×44px; focus ring 2px primary, offset 2px.

## 10. Interaction contract

### Navigation

- `routerLinkActive` match exact cho Overview để tránh active sai.
- Điều hướng thành công đóng mobile drawer và đưa focus tới page heading/main content.
- Route đang active có `aria-current="page"`.
- Compact sidebar hiển thị tooltip cho label bằng keyboard và pointer.

### Mobile drawer

1. Nút menu mở drawer và lưu trigger hiện tại.
2. Focus chuyển vào drawer.
3. Background scroll bị khóa; backdrop không nhận tab focus.
4. `Escape`, backdrop hoặc chọn route đóng drawer.
5. Khi đóng bằng Escape/backdrop, focus trở về menu button.

### Search/notification/profile

- Search và notification chưa có contract phải ẩn hoặc disabled với copy rõ “Sắp có”.
- Disabled control không hiển thị result/badge giả.
- Profile menu chỉ dùng dữ liệu session/API thật; loading/error có state riêng.

### Session và permission

- Session expired lưu URL hiện tại, chuyển `/admin/login?returnUrl=...` và quay lại sau login.
- 403 hiển thị forbidden state với đường quay về trang admin hợp lệ.
- Action/nav không có quyền không được render; guard vẫn bảo vệ direct URL.

## 11. Accessibility acceptance

- Có skip link tới `#admin-main`.
- Landmarks có accessible name khi cần; chỉ có một main landmark chịu trách nhiệm cho page workspace.
- Mọi icon-only button có `aria-label`; bỏ `title="btn"` không có nghĩa.
- Mobile menu/profile trigger có `aria-expanded` và `aria-controls`.
- Drawer/menu quản lý focus và đóng bằng Escape.
- Tab order theo thứ tự thị giác: skip link → sidebar/top bar → page content.
- Active page có `aria-current="page"`; không chỉ thể hiện bằng màu.
- Contrast đạt WCAG AA; zoom 200% không che navigation hoặc page actions.
- Motion tôn trọng `prefers-reduced-motion`.

## 12. Responsive acceptance

| Kích thước | Acceptance |
|---|---|
| 1440px | Sidebar 256px, top bar/canvas offset đúng, không horizontal scroll toàn trang |
| 1024px | Sidebar 72px; label qua tooltip; content còn đủ chiều rộng và padding 24px |
| 768px | Sidebar không chiếm layout; menu button mở modal drawer; top bar không overflow |
| 390px | Logo/title truncate hợp lý; button ≥44px; breadcrumb không đẩy action khỏi màn hình |
| 200% zoom | Drawer, profile menu, logout và router content vẫn tiếp cận được |

## 13. Error và loading states

| Vùng | Loading | Error | Empty/Unavailable |
|---|---|---|---|
| Route/page content | Page-level skeleton do page con quản lý | Scoped page error; shell vẫn hoạt động | Router fallback/404 trong shell nếu phù hợp |
| Profile | Skeleton compact giữ layout | Ẩn dữ liệu nhạy cảm, retry hoặc chỉ giữ logout | Không dùng placeholder giả |
| Notification | Không hiện unread dot | Trạng thái kết nối kín đáo khi capability có thật | Ẩn/disabled “Sắp có” trước contract |
| Permission | Guard đang kiểm tra | Forbidden state cho 403 | Mục không quyền bị ẩn |
| Logout | Disable trigger/spinner | Giữ session nếu server logout thất bại theo security policy | Không điều hướng lặp |

Shell không được chặn toàn bộ navigation chỉ vì một widget top bar lỗi.

## 14. Test plan

### Component tests

- Render đúng group/item Available theo navigation config.
- Top-bar title và breadcrumb thay đổi khi chuyển từng child route.
- Overview active exact; các route khác chỉ active đúng item tương ứng.
- Mobile menu mở/đóng, Escape hoạt động và focus quay trigger.
- Chọn route trên mobile đóng drawer và mở đúng trang.
- Logout gọi service đúng một lần và xử lý pending/error.
- Profile/notification placeholder không xuất hiện khi chưa có contract.

### Route/guard integration tests

- Không có token/admin role thì chuyển login với `returnUrl`.
- Admin role truy cập được các route Available.
- Route metadata có title, breadcrumb và capability/permission field yêu cầu.
- Direct URL không quyền trả forbidden khi RBAC được triển khai.
- Login route không render Dashboard shell.

### Accessibility/responsive tests

- Axe/WCAG cho desktop shell, compact sidebar và mobile drawer.
- Keyboard-only: skip link, navigation, profile, logout, drawer.
- Visual check tại 1440, 1024, 768, 390px và zoom 200%.
- Kiểm tra `prefers-reduced-motion`.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false
```

## 15. Checklist review sau mỗi lần nâng UI

- [ ] Shell chỉ hiển thị dữ liệu profile/notification từ nguồn thật.
- [ ] Title và breadcrumb lấy từ route, không hard-code.
- [ ] Navigation được nhóm và chỉ chứa capability đủ điều kiện.
- [ ] Mục không có quyền bị ẩn; direct route vẫn có guard.
- [ ] Không còn raw hex hoặc icon set lẫn lộn trong shell.
- [ ] Active state có `aria-current`, label/icon và semantic token.
- [ ] Sidebar đạt 256px/72px/drawer đúng breakpoint.
- [ ] Mobile drawer lock scroll, Escape, focus trap và focus return hoạt động.
- [ ] Search/notification chưa có contract được ẩn hoặc ghi rõ “Sắp có”.
- [ ] Canvas không cắt scroll/nội dung trang con.
- [ ] Skip link, landmarks, focus ring và touch target đạt yêu cầu.
- [ ] Session expired, forbidden và logout được kiểm tra.
- [ ] Kiểm tra 1440/1024/768/390px và zoom 200%.
- [ ] Chạy build/test và đính kèm screenshot shell desktop/mobile vào PR.

## 16. Definition of done cho Dashboard Shell

Dashboard shell hoàn thành khi toàn bộ P0 và P1 đạt, các route Available dùng metadata nhất quán, responsive/a11y tests ở §14 chạy thành công và không có profile, notification, badge hoặc navigation Proposed nào dùng dữ liệu giả. Các mục P2 chỉ chuyển sang hoạt động sau khi API, permission, state và test contract tương ứng được triển khai.
