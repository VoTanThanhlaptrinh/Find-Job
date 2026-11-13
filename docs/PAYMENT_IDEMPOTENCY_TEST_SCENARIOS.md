# Bộ Kịch Bản Kiểm Thử API Payment & Redis Idempotency (Postman Guide)

## 1. Chuẩn bị trước khi chạy

1. Khởi động **PostgreSQL** và **Redis**, sau đó chạy backend `job-backend` tại `http://localhost:8080`.
2. Đảm bảo đã có sẵn một tài khoản user đã kích hoạt trong database để đăng nhập ở bước `TC-00` (mặc định đang gọi `POST /api/auth/user/login`; nếu dùng tài khoản nhà tuyển dụng, đổi sang `/api/auth/hirer/login`).
3. Import file Collection vào Postman:
   - Đường dẫn file: [Payment_Idempotency.postman_collection.json](file:///d:/web-project/docs/postman/Payment_Idempotency.postman_collection.json)
4. Mở tab **Variables** của Collection vừa import và cập nhật `username`, `password` (và `baseUrl` nếu không phải `http://localhost:8080`).

---

## 2. Danh sách các kịch bản kiểm thử (TC-00 -> TC-09)

| Mã TC | Tên kịch bản | Method & Path | Header `Idempotency-Key` | Kết quả kỳ vọng | Mục đích kiểm chứng |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-00** | Đăng nhập lấy JWT | `POST /api/auth/user/login` | Không cần | `200 OK`, tự động lưu `accessToken` | Xác thực cho các API `/api/payments/**` |
| **TC-01** | Thiếu header `Idempotency-Key` | `POST /api/payments` | Không gửi | `400 Bad Request` | API nằm trong `app.idempotency.endpoints` bắt buộc phải có key |
| **TC-02** | Header toàn khoảng trắng | `POST /api/payments` | `"    "` | `400 Bad Request` | Reject key rỗng ngay tại Filter |
| **TC-03** | Key dài > 128 ký tự | `POST /api/payments` | Chuỗi 130 ký tự `'k'` | `400 Bad Request` | Bảo vệ độ dài key Redis (`MAX_KEY_LENGTH = 128`) |
| **TC-04** | Tạo Payment hợp lệ lần 1 | `POST /api/payments` | UUID mới (`{{$guid}}`) | `201 Created`, header `X-Idempotent-Replayed: false` | Ghi mới 1 dòng vào bảng `payments` và cache kết quả vào Redis |
| **TC-05** | Gửi lại cùng key + cùng payload | `POST /api/payments` | Giữ nguyên key TC-04 | `201 Created`, header `X-Idempotent-Replayed: true`, trả y hệt `id` & `transactionCode` của TC-04 | Chứng minh **Response Replay** từ Redis, không ghi thêm dòng mới vào DB |
| **TC-06** | Trùng key nhưng đổi `amount` | `POST /api/payments` | Giữ nguyên key TC-04 | `409 Conflict` (`different request payload`) | Chứng minh kiểm tra chữ ký SHA-256 (`requestHash`) |
| **TC-07A** | Gửi request lỗi validation (`amount: -100`) | `POST /api/payments` | UUID mới | `400 Bad Request` | Filter phải giải phóng lock (`releaseLock`) khi status $\ge 400$ |
| **TC-07B** | Retry cùng key TC-07A sau khi sửa `amount: 250000` | `POST /api/payments` | Giữ nguyên key TC-07A | `201 Created`, `X-Idempotent-Replayed: false` | Chứng minh người dùng có thể sửa lỗi và gửi lại cùng key |
| **TC-08** | Lấy chi tiết giao dịch | `GET /api/payments/{id}` | Không gửi | `200 OK` | Endpoint ngoài danh sách `app.idempotency.endpoints` chạy bình thường |
| **TC-09** | Lấy danh sách giao dịch cá nhân | `GET /api/payments/me` | Không gửi | `200 OK`, chỉ có **đúng 1** bản ghi có `transactionCode` của TC-04 | Kiểm chứng cuối cùng trên DB: không hề tạo giao dịch trùng |

---

## 3. Cách chạy tự động trên Postman

1. Trong Postman, chọn Collection **Payment & Redis Idempotency API Test Suite** -> bấm nút **Run** (góc trên bên phải) để mở **Collection Runner**.
2. Kiểm tra thứ tự các request giữ nguyên từ `TC-00` đến `TC-09` (bỏ tick những request không muốn chạy nếu cần, nhưng `TC-04` -> `TC-05` -> `TC-06` -> `TC-08` -> `TC-09` phụ thuộc biến của nhau).
3. Bấm **Run Payment & Redis Idempotency API Test Suite**.
4. Quan sát kết quả **All Tests Passed** (tổng cộng 18 assertions).

---

## 4. Kiểm tra thêm trường hợp Concurrent (`IN_PROGRESS` -> `409 Conflict`)

Do Postman Collection Runner chạy tuần tự từng request, để test tình huống 2 request gửi **đồng thời** khi request đầu chưa chạy xong:
1. Tạm thời thêm `Thread.sleep(3000)` ở đầu hàm `createPayment` trong `PaymentServiceImpl.java` (hoặc đặt breakpoint debug tại Controller).
2. Mở 2 tab Postman dùng chung một giá trị header `Idempotency-Key` và cùng JSON body.
3. Bấm **Send** ở Tab 1, ngay lập tức bấm **Send** ở Tab 2 trong vòng 3 giây -> Tab 2 sẽ nhận về **`409 Conflict`** với thông báo `"A request with this Idempotency-Key is currently in progress"`.
4. (Nhớ xóa `Thread.sleep` sau khi test xong).

