---
name: company-address-management-ui
description: 'Build company-address management UI in Angular: add route, render address list, place top-right add button, add per-item kebab menu with delete action, and seed mock data for layout verification. Use when creating or refining employer address pages.'
argument-hint: 'Describe feature path and expected address fields'
user-invocable: true
---

# Company Address Management UI

## Khi Nao Su Dung
- Tao moi trang quan ly dia chi cong ty cho recruiter/employer.
- Can cap nhat route truoc khi wiring API.
- Can mock data de review layout voi PM/QA truoc.

## Dau Vao Can Co
- Duong dan feature page (vi du: `features/employer/pages/company-address`).
- Route parent hien tai trong app.
- Danh sach field can hien thi tren 1 dia chi.

## Quy Trinh Thuc Hien
1. Xac nhan route va component dich.
2. Cap nhat route config de truy cap duoc trang company-address.
3. Dung du lieu mau trong component TS:
   - Tao mang dia chi co it nhat 3 item.
   - Moi item co id, ten dia diem, nguoi lien he, so dien thoai, dia chi day du, mac dinh/khong.
4. Tao UI trang:
   - Header trang voi tieu de.
   - Nut Them dia chi o goc tren ben phai.
   - Danh sach card dia chi ben duoi.
   - Moi card co thong tin dia chi o ben trai.
   - Ben phai tren cua card co nut `...` (kebab menu).
   - Mo menu thi hien hanh dong Xoa.
5. Them state UI toi thieu:
   - Theo doi menu dang mo theo `id`.
   - Dong menu khi click lai cung item hoac click item khac.
6. Kiem tra responsive:
   - Desktop: thong tin va menu hien ro.
   - Mobile: card khong vo layout, nut them van de thay.

## Tieu Chi Hoan Tat
- Route moi truy cap duoc khong loi.
- Trang hien danh sach dia chi tu mock data.
- Nut Them dia chi o dung vi tri goc tren ben phai.
- Menu `...` moi card mo duoc va co hanh dong Xoa.
- Khong can API that de xem duoc layout hoan chinh.

## Nhanh Quyet Dinh
- Neu route da ton tai: chi chinh UI + mock data.
- Neu da co design system: dung component co san, khong pha vo style hien tai.
- Neu chua co API contract: giu mock data typed model trong component hoac file model gan feature.

## Kiem Tra Sau Cung
1. Chay app va truy cap route company-address.
2. Thu mo menu `...` tren nhieu card lien tiep.
3. Xac nhan button Them dia chi khong bi tran o man hinh nho.
4. Chup screenshot de review layout neu can.

## Prompt Mau
- `/company-address-management-ui Them trang company-address, route /recruiter/dashboard/company-address, mock 4 dia chi va dung card list co menu xoa.`
- `/company-address-management-ui Chinh sua layout company-address de nut them nam goc phai tren va menu ... mo dung tung item.`
