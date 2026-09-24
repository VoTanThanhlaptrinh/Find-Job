# Kế hoạch cải thiện UI — Admin Metric Card & Page Header

> Component: `AdminMetricCardComponent`, `AdminPageHeaderComponent`  
> Nguồn thiết kế: `docs/ADMIN_UI_DESIGN.md` §3–5, §8–10  
> Phạm vi frontend: `job-frontend/src/app/features/admin/components/shared/metric-card/` và `page-header/`  
> Trạng thái sử dụng: component đã tồn tại nhưng chưa được các trang admin sử dụng

## 1. Mục tiêu và giá trị mang lại

Hai component này phải trở thành primitive UI thống nhất cho toàn bộ Admin Portal:

1. `AdminPageHeader` cung cấp breadcrumb, tiêu đề, mô tả và vùng action nhất quán.
2. `AdminMetricCard` hiển thị KPI, trend, kỳ so sánh, icon và skeleton đúng cùng một contract.
3. Trang admin không còn lặp lại markup/header/card với màu, spacing và trạng thái khác nhau.
4. Component bảo đảm accessibility và responsive mặc định, thay vì yêu cầu từng trang tự xử lý.
5. Consumer chỉ truyền dữ liệu và hành vi; không truyền chuỗi class để thay đổi cấu trúc tùy ý.

Việc chuẩn hóa component không được sửa nghĩa dữ liệu. Trend, comparison period, permission và action state phải đến từ contract của trang/API, không được component suy diễn.

## 2. Hiện trạng và khoảng cách cần xử lý

### AdminMetricCard

| Hiện trạng | Khoảng cách với Admin Design | Tác động |
|---|---|---|
| Root là `div` | KPI là một đơn vị nội dung độc lập, nên có semantics phù hợp | Screen reader khó nhận biết cấu trúc card |
| Chỉ có `label`, `value`, `hint` | Thiếu trend direction, semantic tone, comparison period và loading | Consumer phải tự dựng footer, dễ không nhất quán |
| `hint` luôn mặc định màu emerald | Trend âm/cảnh báo/trung tính có thể bị biểu diễn sai | Màu truyền đạt sai ý nghĩa dữ liệu |
| Value là `string` | Không nói rõ format/unknown/aria label | Dễ nối chuỗi tiền, phần trăm hoặc dấu `+` sai |
| Không có skeleton tích hợp | Mỗi trang tạo skeleton khác kích thước | Layout shift và trải nghiệm loading không đồng nhất |
| Cho override năm chuỗi class | API component bị rò rỉ implementation | Trang có thể phá token, spacing, responsive và Tailwind build |
| Dùng `bg-white`, `slate-*`, shadow | Không theo semantic `ad-*`; card mặc định không nên có shadow | Khó đổi theme và lệch đặc tả |
| Dùng `ad-rounded-xl` | Không thấy utility này trong design token hiện tại | Radius có thể không được áp dụng |

### AdminPageHeader

| Hiện trạng | Khoảng cách với Admin Design | Tác động |
|---|---|---|
| Root là `div` | Thiếu landmark `header` | Cấu trúc trang kém rõ cho assistive technology |
| Chỉ có title/description | Thiếu breadcrumb | Không đáp ứng contract `AdminPageHeader` |
| Một action dựng nội bộ | Thiếu tối đa hai secondary actions và trạng thái loading/disabled | Không dùng được cho refresh, range, export hoặc menu |
| Button không có `@Output` rõ ràng | Consumer không có contract hành vi trực tiếp | Action có thể xuất hiện nhưng không thực hiện nghiệp vụ |
| Icon không `aria-hidden` | Screen reader có thể đọc tên glyph không cần thiết | Accessible name bị nhiễu |
| Layout luôn `justify-between items-end` | Có thể tràn ở mobile hoặc title/action dài | Action bị đẩy khỏi viewport |
| Cho override toàn bộ class | Mỗi trang có thể tạo một kiểu header riêng | Shared component mất giá trị chuẩn hóa |
| Dùng raw `blue-*`, `slate-*`, shadow/scale | Lệch semantic token, motion và reduced-motion rule | Theme, contrast và hành vi không thống nhất |

## 3. Nguyên tắc API component

- Component chịu trách nhiệm về layout, semantic HTML, token, focus và responsive.
- Consumer chịu trách nhiệm về dữ liệu đã format, permission và callback nghiệp vụ.
- Không nhận raw class string cho container/title/value/action trong public API.
- Chỉ thêm variant hữu hạn khi có ít nhất hai use case thật.
- Dữ liệu không có phải hiển thị unknown/empty rõ ràng, không mặc định thành `0`, `+0%` hoặc trend tốt.
- Trend direction và business semantic là hai khái niệm khác nhau. Ví dụ pending jobs giảm là hướng xuống nhưng có thể mang semantic tích cực.
- Skeleton phải giữ kích thước gần bằng content thật và tôn trọng reduced motion.
- Icon trang trí dùng `aria-hidden`; icon mang thông tin phải có accessible label.

## 4. Contract đích — AdminMetricCard

### Nội dung bắt buộc và tùy chọn

```text
┌──────────────────────────────────┐
│ LABEL                      [icon] │
│ 12.450                           │
│ [↑ 8,2%] so với 30 ngày trước   │
└──────────────────────────────────┘
```

- `label`: bắt buộc, ngắn và mô tả chính xác KPI.
- `displayValue`: bắt buộc khi không loading; đã format theo locale/currency/percentage ở tầng phù hợp.
- `valueAriaLabel`: tùy chọn khi display value viết tắt hoặc ký hiệu khó đọc.
- `trend`: tùy chọn, gồm value, direction, semantic tone và comparison label.
- `icon`: content projection; trang trí mặc định.
- `loading`: render skeleton cùng kích thước card.
- `density`: chỉ cân nhắc `default | compact` khi có use case thật; chưa cần ở P0.

### Type đề xuất

```ts
export type MetricTrendDirection = 'up' | 'down' | 'flat';
export type MetricTrendTone = 'positive' | 'negative' | 'warning' | 'neutral';

export interface AdminMetricTrend {
  displayValue: string;
  direction: MetricTrendDirection;
  tone: MetricTrendTone;
  comparisonLabel: string;
  ariaLabel?: string;
}

export interface AdminMetricCardViewModel {
  label: string;
  displayValue: string;
  valueAriaLabel?: string;
  trend?: AdminMetricTrend;
}
```

Không tự chọn tone từ dấu của trend. Consumer phải truyền tone theo ý nghĩa nghiệp vụ đã xác định.

### Semantics đề xuất

```html
<article [attr.aria-label]="label" [attr.aria-busy]="loading || null">
  <header>
    <p>{{ label }}</p>
    <ng-content select="[metric-icon]"></ng-content>
  </header>

  <p [attr.aria-label]="valueAriaLabel || null">{{ displayValue }}</p>

  <p *ngIf="trend">
    <span><!-- direction icon + displayValue --></span>
    <span>{{ trend.comparisonLabel }}</span>
  </p>
</article>
```

Markup thực tế có thể dùng Angular control flow hiện tại của project, nhưng phải giữ semantics và accessible name tương đương.

## 5. Contract đích — AdminPageHeader

### Bố cục

```text
[Admin / Nhóm / Trang hiện tại]
[Tiêu đề trang]                         [Secondary] [Secondary] [Primary]
[Mô tả ngắn]
```

- Breadcrumb nằm trên title, có `nav[aria-label="Đường dẫn trang"]`.
- Title là `h1`; mỗi page chỉ có một `h1`.
- Description ngắn, giải thích mục tiêu trang thay vì mô tả endpoint kỹ thuật.
- Tối đa một primary action và hai secondary actions.
- Header chỉ render action mà user có permission; không dùng disabled để thay cho permission denied.

### Type breadcrumb đề xuất

```ts
export interface AdminBreadcrumbItem {
  label: string;
  routerLink?: string | readonly unknown[];
}
```

Item cuối không có link và dùng `aria-current="page"`. Breadcrumb có thể nhận từ route metadata hoặc page facade; không hard-code ở shell và page theo hai nguồn khác nhau.

### Action API khuyến nghị

Page header chịu layout action bằng content projection:

```html
<app-admin-page-header
  [title]="pageTitle"
  [description]="pageDescription"
  [breadcrumbs]="breadcrumbs"
>
  <button header-secondary-action type="button">Làm mới</button>
  <button header-primary-action type="button">Tạo tin tuyển dụng</button>
</app-admin-page-header>
```

Lý do dùng content projection:

- Consumer giữ được event handler, router link, permission, disabled/loading và accessible label.
- Header không phải tạo một action config phức tạp cho button, link, menu và segmented control.
- Có thể dùng shared button primitive sau này mà không đổi API header.

Component vẫn phải định style/layout cho slot hoặc cung cấp documented button variant; không cho consumer truyền `actionClass` tùy ý.

## 6. Danh sách nhiệm vụ triển khai

### P0 — Chuẩn hóa contract và semantics

| ID | Component | Nhiệm vụ | Điều kiện hoàn thành |
|---|---|---|---|
| SC-01 | Cả hai | Loại bỏ public class-string inputs | Không còn `containerClass`, `labelClass`, `valueClass`, `wrapperClass`, `actionClass`… trong API |
| SC-02 | Cả hai | Chuyển sang semantic `ad-*` tokens | Không còn raw `white/slate/blue/emerald`; card dùng border, không shadow mặc định |
| SC-03 | MetricCard | Đổi root thành `article` và chuẩn hóa hierarchy | Label/value/trend có semantics; icon decorative được ẩn khỏi accessibility tree |
| SC-04 | MetricCard | Thay `hint` bằng typed trend | Có direction, tone, comparison label và unknown behavior; tone không suy ra tự động |
| SC-05 | MetricCard | Thêm skeleton tích hợp | `loading=true` giữ chiều cao card, có `aria-busy`; không render fake value |
| SC-06 | MetricCard | Chuẩn hóa value formatting contract | Value dùng tabular numerals; currency/number/percent không nối chuỗi sai; hỗ trợ aria label |
| SC-07 | PageHeader | Dùng landmark `header`, một `h1` và description | Title required; description tùy chọn; không tạo `h1` thứ hai trong page |
| SC-08 | PageHeader | Thêm typed breadcrumb | `nav` label đúng; item cuối `aria-current`; link dùng RouterLink và focus-visible |
| SC-09 | PageHeader | Thay button nội bộ bằng action slots | Hỗ trợ tối đa 1 primary + 2 secondary; consumer giữ callback/loading/permission |
| SC-10 | PageHeader | Chuẩn hóa responsive layout | Mobile xếp dọc; action wrap/full-width hợp lý; title dài không gây overflow |
| SC-11 | Cả hai | Thêm component specs | Test content, semantics, loading, trend, breadcrumb, slots và responsive class contract |
| SC-12 | Cả hai | Viết usage examples | Có ví dụ tối thiểu: KPI thường/loading/no-trend và header no-action/primary/multiple actions |

### P1 — Độ bền, accessibility và migration

| ID | Component | Nhiệm vụ | Điều kiện hoàn thành |
|---|---|---|---|
| SC-13 | MetricCard | Hỗ trợ value dài và localized text | Không overflow với currency/số lớn; label/comparison tiếng Việt dài vẫn đọc được |
| SC-14 | MetricCard | Chuẩn hóa tone mapping | Positive/success, negative/error, warning, neutral đều có icon/text, không chỉ màu |
| SC-15 | MetricCard | Reduced-motion skeleton | Tắt pulse/transition không cần thiết với `prefers-reduced-motion` |
| SC-16 | PageHeader | Chuẩn hóa action ordering | Secondary trước primary; DOM order trùng visual/tab order |
| SC-17 | PageHeader | Xử lý breadcrumb dài | Wrap/truncate có accessible full label; không đẩy action khỏi viewport |
| SC-18 | PageHeader | Hỗ trợ loading/disabled từ consumer | Action giữ accessible name và state; spinner decorative; target mobile ≥44px |
| SC-19 | Cả hai | Migrate các trang Available | Overview, Employers, Job Seekers, Jobs và Billing dùng component thay markup lặp |
| SC-20 | Cả hai | Visual regression/responsive review | So sánh 1440/1024/768/390px, zoom 200%, dark/theme chỉ khi project hỗ trợ |

### P2 — Chỉ thêm khi có use case thực tế

| ID | Component | Capability | Điều kiện trước khi thêm |
|---|---|---|---|
| SC-21 | MetricCard | Compact density | Có ít nhất hai màn hình cần card thấp hơn và acceptance spacing rõ |
| SC-22 | MetricCard | Clickable/drill-down card | Có route/action thật; toàn card có semantics link/button, keyboard và focus rõ |
| SC-23 | MetricCard | Sparkline | Có time-series API, accessible summary và data fallback |
| SC-24 | PageHeader | Overflow action menu | Trang thực sự vượt giới hạn 1 primary + 2 secondary |
| SC-25 | PageHeader | Sticky header | Có UX evidence; xử lý shell offset, shadow khi stuck và focus visibility |

Không thêm variant cho một trang đơn lẻ nếu content projection hoặc composition đã giải quyết được.

## 7. Visual specification

### MetricCard

| Thuộc tính | Quy chuẩn |
|---|---|
| Surface | `ad-surface-container-lowest` |
| Border | `ad-outline-variant`, độ nhấn thấp |
| Radius | 12px (`rounded-xl`) |
| Padding | 20px desktop; 16px khi layout hẹp nếu cần |
| Shadow | Không có mặc định |
| Label | Inter 12/18, semibold; sentence case ưu tiên hơn uppercase kéo giãn |
| Value | Manrope 30/38 hoặc 24/32 theo mật độ; semibold/bold; `tabular-nums` |
| Trend | Inter 12/18; badge/icon + text; comparison dùng `ad-on-surface-variant` |
| Icon box | 32–36px; token surface/tone phù hợp; icon set thống nhất |
| Min height | Cố định theo variant để skeleton/content không làm grid nhảy |

### PageHeader

| Thuộc tính | Quy chuẩn |
|---|---|
| Title | Manrope 30/38 semibold; mobile có thể 24/32 |
| Description | Inter 14/22; `ad-on-surface-variant`; max-width để dễ đọc |
| Breadcrumb | 12/18 hoặc 13/20; link `ad-on-surface-variant`, current `ad-on-surface` |
| Spacing | Breadcrumb → title 4–8px; title → description 4px; header → content 24–32px |
| Primary action | `ad-primary` / `ad-on-primary`; hover `ad-primary-dim`; radius 8px |
| Secondary action | Surface/border variant; không cạnh tranh primary |
| Shadow | Không có; chỉ dùng nếu sticky và đang ở trạng thái stuck |

Giá trị token thực tế trong Tailwind/CSS phải được đồng bộ với bảng §3 của `ADMIN_UI_DESIGN.md`. Component không hard-code màu để bù cho token cấu hình sai.

## 8. State contract

### MetricCard

| State | Hành vi |
|---|---|
| Loading lần đầu | Skeleton label/icon/value/footer, giữ kích thước; `aria-busy=true` |
| Refreshing | Giữ value cũ; section cha thông báo refresh nhẹ; không nhấp nháy về skeleton |
| Success | Render value/trend thật, format đúng locale |
| Unknown value | Hiển thị “Chưa có dữ liệu” hoặc em dash có accessible label; không dùng 0 |
| No trend | Không render footer rỗng; card vẫn giữ layout cân bằng theo grid |
| Error | Scoped error do section/page sở hữu; card không tự biến thành 0 |

### PageHeader

| State | Hành vi |
|---|---|
| Không có action | Text block dùng toàn chiều ngang, không chừa vùng trống |
| Một primary | Desktop căn phải; mobile full-width hoặc theo acceptance của trang |
| Secondary + primary | DOM/tab order secondary trước primary; wrap không chồng lấn |
| Action loading | Button consumer disable, có progress label và chống click lặp |
| Permission denied | Không project action; không render disabled action để lộ capability |
| Long title/breadcrumb | Wrap/truncate có chủ đích, vẫn đọc được ở zoom 200% |

## 9. Accessibility acceptance

### MetricCard

- Root là `article` hoặc cấu trúc tương đương trong section có heading.
- Label và value được đọc theo thứ tự hợp lý.
- Icon trend có text/aria label; màu không phải tín hiệu duy nhất.
- Icon trang trí có `aria-hidden="true"`.
- Skeleton có accessible loading label ở container cha hoặc card; skeleton shapes không được đọc.
- `valueAriaLabel` mô tả đầy đủ các dạng viết tắt như `₫1,2 tỷ`, `8,2%` khi cần.

### PageHeader

- Root là `header`; breadcrumb là `nav` có accessible label.
- Page có đúng một `h1`; title không rỗng.
- Breadcrumb current item có `aria-current="page"`; separator decorative.
- Action icon decorative; button/link có accessible name rõ.
- Tab order trùng visual order; focus ring 2px, offset 2px, contrast đạt chuẩn.
- Action touch target tối thiểu 44px tại 390px.

## 10. Responsive acceptance

| Breakpoint | MetricCard | PageHeader |
|---|---|---|
| 1440px | Hỗ trợ grid tối đa 4–5 cột, chiều cao card đồng đều | Text trái, actions phải; không vượt content 1440px |
| 1024px | Grid consumer 2–4 cột theo trang; value không overflow | Actions wrap có chủ đích, không đẩy title khỏi vùng nhìn |
| 768px | Card giữ hierarchy, icon không chiếm chỗ value | Header chuyển column khi thiếu chỗ; actions có thể wrap/full-width |
| 390px | Một cột hoặc snap do page quyết định; padding hợp lý | Breadcrumb/title/description/actions xếp dọc; CTA ≥44px |
| Zoom 200% | Không cắt value/trend/comparison | Không mất breadcrumb, title hoặc action; không overlap |

Component không quyết định số cột KPI; grid layout thuộc page/section. Component chỉ bảo đảm card co giãn an toàn trong container.

## 11. Kế hoạch migration

### Giai đoạn 1 — Hoàn thiện component

1. Refactor API và template của hai component.
2. Thêm specs trước khi migrate consumer.
3. Xác nhận semantic tokens trong Tailwind/CSS khớp Admin Design.
4. Tạo usage examples trong tài liệu hoặc Storybook nếu project có.

### Giai đoạn 2 — Migrate theo trang

1. Overview: header có breadcrumb/range/refresh; 4 KPI.
2. Employers: header có export; 4 KPI.
3. Job Seekers: header có create; 5 KPI.
4. Jobs Management: header có create; 4 KPI.
5. Billing: header và 3 KPI.

Mỗi trang migrate trong một thay đổi nhỏ, giữ nguyên data/service behavior. Sau mỗi trang phải chạy checklist của tài liệu trang tương ứng và checklist §13 bên dưới.

### Giai đoạn 3 — Dọn markup trùng

- Chỉ xóa markup/card styles cũ sau khi screenshot và tests xác nhận parity.
- Không giữ song song hai implementation lâu dài.
- Nếu một trang cần khác biệt, ưu tiên composition/slot; chỉ thêm variant shared khi có use case lặp lại.

## 12. Test plan

### AdminMetricCard specs

- Render label và formatted value đúng.
- No trend không tạo footer/spacing lỗi.
- Up/down/flat render đúng icon, display value, tone và comparison text.
- Tone không bị suy ra từ direction.
- Loading render skeleton, `aria-busy`, không render value giả.
- Unknown value có accessible text và không thành 0.
- Projected icon/footer nếu còn hỗ trợ có semantics đúng.
- Value rất dài và comparison text dài không overflow container.

### AdminPageHeader specs

- Render đúng một `h1`, description tùy chọn và header landmark.
- Breadcrumb link/current/separator và `aria-current` đúng.
- Không action không để layout rỗng.
- Primary/secondary slots đúng DOM order và giới hạn documented.
- Click/action handler thuộc consumer vẫn hoạt động.
- Loading/disabled/permission state của projected action được giữ.
- Title/breadcrumb/action dài wrap đúng responsive classes.

### Integration/visual/a11y

- Migrate từng page không thay đổi request/API behavior.
- Axe/WCAG cho header no-action, multi-action; metric loading/no-trend/all trend tones.
- Keyboard-only qua breadcrumb và actions.
- Visual check 1440/1024/768/390px và zoom 200%.
- Reduced-motion không chạy pulse/scale animation không cần thiết.

### Commands

```bash
cd job-frontend
npm run build
npm test -- --watch=false
```

## 13. Checklist review sau mỗi lần nâng UI

- [ ] Đối chiếu thay đổi với `ADMIN_UI_DESIGN.md` §3–5, §8–10.
- [ ] Không còn public class-string input làm API styling.
- [ ] Chỉ dùng semantic `ad-*` token; không raw palette hoặc shadow card mặc định.
- [ ] Mỗi page có đúng một `h1`; PageHeader dùng `header` và breadcrumb semantic.
- [ ] PageHeader có tối đa 1 primary và 2 secondary actions.
- [ ] Action permission/loading/disabled/click do consumer kiểm soát và hoạt động thật.
- [ ] MetricCard có typed trend, comparison period và semantic tone độc lập direction.
- [ ] KPI dùng tabular numerals và formatter phù hợp; unknown không thành 0.
- [ ] Skeleton giữ kích thước, có loading semantics và reduced-motion.
- [ ] Icon trang trí được ẩn; trend/status không truyền đạt chỉ bằng màu.
- [ ] Mobile action ≥44px; title/value/text dài không overflow.
- [ ] Kiểm tra keyboard, axe, 1440/1024/768/390px và zoom 200%.
- [ ] Trang được migrate chạy cả checklist UI riêng của trang đó.
- [ ] Chạy frontend build/test và lưu screenshot/evidence trong PR.

## 14. Definition of done

Hai shared component được xem là hoàn thành khi:

1. Toàn bộ P0 và P1 đạt acceptance criteria, có component specs.
2. API không còn nhận chuỗi class tùy ý và không chứa raw palette trái design system.
3. `AdminMetricCard` xử lý value/trend/comparison/loading/unknown đúng semantics.
4. `AdminPageHeader` xử lý breadcrumb/title/description/action slots đúng accessibility và responsive.
5. Các trang Available đã migrate mà không thay đổi data contract hoặc hành vi nghiệp vụ.
6. Test plan §12 chạy thành công và checklist §13 được xác nhận sau từng lần migrate.
