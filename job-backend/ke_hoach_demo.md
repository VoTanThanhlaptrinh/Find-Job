# 📋 Kế Hoạch Demo — Báo cáo Tiểu Luận Tốt Nghiệp

> **Chủ đề:** NGHIÊN CỨU KỸ THUẬT TÌM KIẾM NGỮ NGHĨA CHO BÀI TOÁN GỢI Ý VIỆC LÀM DỰA TRÊN HỒ SƠ ỨNG VIÊN
>
> **Hình thức:** Demo live trên web đã deploy (Vercel + Render)
>
> **Đối tượng:** Giáo viên phản biện — Báo cáo tiểu luận tốt nghiệp

---

## Chiến lược tổng thể

Chia demo thành **3 phần** theo nguyên tắc: _"Bối cảnh → Trọng tâm → Nền tảng"_

| Phần | Nội dung | Thời gian | Mức ưu tiên |
|------|----------|-----------|-------------|
| **Phần 1** | Tổng quan hệ thống (lướt nhanh) | ~3 phút | Phụ |
| **Phần 2** | **TRỌNG TÂM — Luồng AI Semantic Search** | ~10 phút | **Chính** |
| **Phần 3** | Các chức năng hỗ trợ còn lại | ~3 phút | Phụ |

> [!IMPORTANT]
> **Nguyên tắc vàng:** Hội đồng đánh giá dựa trên chủ đề tiểu luận. Phần 2 (AI Matching) phải chiếm **~60% thời gian demo** và là phần bạn tự tin nhất.

---

## Phần 1 — Tổng quan hệ thống (~3 phút)

> **Mục tiêu:** Cho hội đồng thấy bối cảnh tổng thể — hệ thống hoàn chỉnh, không phải chỉ là một script AI đơn lẻ.

### Bước 1.1 — Trang chủ (Home Page)
- Mở trang chủ [find-job-frontend.vercel.app](https://find-job-frontend.vercel.app/)
- **Giới thiệu ngắn:** _"Đây là hệ thống tuyển dụng trực tuyến với đầy đủ chức năng cho cả Ứng viên lẫn Nhà tuyển dụng. Hệ thống được xây dựng bằng Angular 20 cho Frontend và Spring Boot 3 cho Backend."_
- Lướt nhanh giao diện trang chủ: danh sách công việc mới nhất, thanh tìm kiếm.

### Bước 1.2 — Đăng nhập bằng tài khoản Ứng viên
- Đăng nhập bằng tài khoản test đã chuẩn bị sẵn.
- **Nói:** _"Hệ thống sử dụng xác thực JWT Token với Refresh Token qua HttpOnly Cookie. Ngoài ra, hệ thống cũng hỗ trợ đăng nhập qua Google OAuth2 và xác thực email."_
- **Không cần demo** chi tiết luồng đăng ký hay quên mật khẩu — chỉ đề cập bằng lời.

### Bước 1.3 — Lướt nhanh trang Tìm kiếm công việc
- Vào trang danh sách công việc, thực hiện **1 lần lọc nhanh** (theo địa điểm hoặc mức lương).
- **Nói:** _"Hệ thống hỗ trợ tìm kiếm và lọc công việc theo nhiều tiêu chí sử dụng JPA Specification ở Backend. Tuy nhiên, đây là tìm kiếm truyền thống dựa trên keyword — chưa phải semantic search."_
- **Mục đích ẩn:** Tạo contrast cho Phần 2 — _"tìm kiếm truyền thống vs. tìm kiếm ngữ nghĩa"_.

---

## Phần 2 — TRỌNG TÂM: Luồng AI Semantic Search (~10 phút)

> **Mục tiêu:** Demo toàn bộ pipeline từ Upload CV → AI Parsing → Embedding → Semantic Match → Kết quả gợi ý. Đây là phần quyết định điểm số.

### Bước 2.1 — Upload CV

- Vào trang **Quản lý CV** (`/infor/cv`).
- **Nói:** _"Bước đầu tiên trong pipeline Semantic Search là thu thập dữ liệu đầu vào. Ứng viên upload file CV của mình lên hệ thống."_
- Upload một file CV mẫu (nếu chưa có) **hoặc** chỉ vào CV đã upload sẵn.
- **Giải thích luồng kỹ thuật:** _"Khi CV được upload, hệ thống sẽ thực hiện 3 bước tự động:"_
  1. _"Lưu trữ file lên Cloud (Cloudflare R2) qua RabbitMQ — xử lý bất đồng bộ."_
  2. _"Dùng Apache Tika để trích xuất nội dung văn bản thuần (raw text) từ file PDF/DOCX."_
  3. _"Gửi raw text tới AI Agent để phân tích."_

### Bước 2.2 — AI Parsing (Bóc tách thông tin CV bằng LLM)

- **Nói:** _"Đây là bước quan trọng nhất. Hệ thống sử dụng LangChain4j kết hợp với mô hình DeepSeek (qua OpenAI-compatible API) để bóc tách ngữ nghĩa từ CV."_
- **Giải thích chi tiết các trường được trích xuất:**

| Trường | Ý nghĩa | Vai trò trong Semantic Search |
|--------|---------|-------------------------------|
| `yearOfExperience` | Số năm kinh nghiệm (số nguyên) | Pre-filter: loại bỏ job yêu cầu kinh nghiệm quá cao |
| `title` | Chức danh mong muốn | Metadata tham khảo |
| `city` | Thành phố | Metadata tham khảo |
| `skillsAndProjectsContext` | Khối văn bản tổng hợp kỹ năng + dự án | **Input chính cho Embedding kỹ năng** |
| `experienceContext` | Khối văn bản tổng hợp kinh nghiệm làm việc | **Input chính cho Embedding kinh nghiệm** |

- **Nói:** _"AI Agent được thiết kế để trích xuất theo đúng ngôn ngữ gốc của CV — nếu CV tiếng Anh thì output tiếng Anh, CV tiếng Việt thì output tiếng Việt. Điều này đảm bảo embedding vector không bị nhiễu do dịch thuật."_

### Bước 2.3 — Vectorization (Tạo Embedding Vector)

- **Nói:** _"Sau khi AI trích xuất xong, hệ thống gọi tới AI Agent Service (Python) để tạo Embedding Vector cho từng khối nội dung bằng mô hình Sentence-BERT."_
- **Giải thích 2 loại vector được tạo:**
  - `skill_and_project_embedding` — vector kỹ năng
  - `experience_embedding` — vector kinh nghiệm
- _"Tương tự, mỗi Job Description (JD) cũng được vectorize cùng cấu trúc và lưu vào bảng `jd_vectors`. Quá trình này xảy ra khi Nhà tuyển dụng đăng tin."_
- _"Cả 2 loại vector (CV và JD) được lưu trong PostgreSQL với pgvector extension, cho phép tính cosine distance trực tiếp bằng SQL."_

### Bước 2.4 — Demo kết quả Gợi ý việc làm (AI Matching)

> [!TIP]
> **Đây là "highlight moment" — điểm nhấn của toàn bộ buổi demo.**

- Vào trang **Gợi ý việc làm** (`/infor/recommended-jobs`).
- Chọn một CV đã được phân tích từ dropdown.
- **Chờ kết quả load** (skeleton loading sẽ hiển thị).
- **Khi kết quả hiện ra, giải thích thuật toán 2 giai đoạn:**

#### Stage 1 — Retrieval (Thu hẹp phạm vi)
- _"Hệ thống sử dụng CROSS JOIN LATERAL để lấy 200 JD có `experience_embedding` gần nhất với CV, đồng thời pre-filter theo `required_years_experience ≤ cv_yoe`."_
- _"Mục đích: giảm không gian tìm kiếm từ hàng ngàn job xuống còn 200 ứng viên tiềm năng."_

#### Stage 2 — Rerank (Xếp hạng lại)
- _"Trong 200 kết quả, hệ thống tính thêm `skill_dist` (cosine distance giữa kỹ năng CV và kỹ năng JD)."_
- _"Công thức cuối: `final_dist = exp_dist × 0.5 + skill_dist × 0.5` — trọng số bằng nhau cho kinh nghiệm và kỹ năng."_
- _"Chỉ giữ lại các job có `final_dist < 0.5` (tức Match Score > 50%), giới hạn 10 kết quả tốt nhất."_

- **Chỉ vào từng job card trên màn hình** và đọc Match Score:
  - _"Job này có tổng độ phù hợp **85%**, trong đó kỹ năng phù hợp **90%** và kinh nghiệm phù hợp **80%**."_

### Bước 2.5 — So sánh với Tìm kiếm truyền thống (nếu có thời gian)

- Quay lại trang tìm kiếm thông thường, gõ vài keyword.
- **Nói:** _"Như các thầy/cô thấy, tìm kiếm truyền thống chỉ match chính xác từ khóa. Nếu ứng viên giỏi 'React' nhưng job viết 'ReactJS' hoặc 'React.js' — keyword search sẽ bỏ sót. Semantic search giải quyết vấn đề này vì nó so sánh ở mức ngữ nghĩa, không phải ký tự."_

---

## Phần 3 — Các chức năng hỗ trợ (~3 phút)

> **Mục tiêu:** Chứng minh đây là hệ thống hoàn chỉnh, không chỉ là demo AI.

### Bước 3.1 — Ứng tuyển công việc (lướt nhanh)
- Từ kết quả gợi ý, click vào 1 job → xem chi tiết → bấm nút Ứng tuyển.
- **Nói:** _"Ứng viên có thể ứng tuyển ngay từ kết quả gợi ý, tạo trải nghiệm liền mạch."_

### Bước 3.2 — Blog & Tương tác (đề cập bằng lời)
- _"Hệ thống cũng có module Blog cho phép người dùng chia sẻ bài viết, bình luận và thích."_

### Bước 3.3 — Admin Dashboard (lướt nhanh nếu có thời gian)
- Đăng nhập Admin, lướt qua Dashboard thống kê.
- _"Phần quản trị cho phép quản lý người dùng, tin tuyển dụng và giao dịch."_

---

## Checklist Chuẩn Bị Trước Ngày Demo

- [ ] **Tài khoản test:** Đảm bảo tài khoản Ứng viên đã đăng nhập được trên Vercel
- [ ] **CV đã phân tích:** Có ít nhất 2-3 CV đã được AI phân tích và vectorize xong (sẵn sàng trong dropdown)
- [ ] **Dữ liệu Job:** Đảm bảo database có đủ job đã được vectorize để kết quả matching có ý nghĩa
- [ ] **Kiểm tra Backend:** Truy cập `https://find-job-ctkj.onrender.com` để đảm bảo server không bị sleep (Render free tier sẽ sleep sau 15 phút không hoạt động — **mở web 5 phút trước khi demo**)
- [ ] **Kết nối mạng:** Kiểm tra wifi phòng báo cáo, chuẩn bị hotspot dự phòng
- [ ] **Tab trình duyệt:** Mở sẵn các tab cần thiết theo thứ tự demo
- [ ] **Video dự phòng:** Quay sẵn 1 video demo ngắn phòng trường hợp mạng chậm hoặc server không phản hồi

> [!WARNING]
> **Render Free Tier** có cold start ~30-60 giây khi server vừa thức dậy. Hãy truy cập API backend **ít nhất 5 phút trước** buổi báo cáo để warm up server.

---

## Các Câu Hỏi Phản Biện Dự Kiến & Gợi Ý Trả Lời

| # | Câu hỏi dự kiến | Gợi ý trả lời |
|---|-----------------|----------------|
| 1 | _"Tại sao chọn Sentence-BERT mà không phải mô hình khác?"_ | Sentence-BERT tối ưu cho so sánh semantic similarity giữa 2 đoạn văn bản, nhẹ hơn GPT embedding, phù hợp với bài toán matching CV-JD |
| 2 | _"Cosine distance và cosine similarity khác nhau thế nào?"_ | `similarity = 1 - distance`. Score 85% nghĩa là distance = 0.15 |
| 3 | _"Tại sao chia trọng số 50/50 cho kinh nghiệm và kỹ năng?"_ | Đây là baseline hợp lý, trong tương lai có thể điều chỉnh theo từng ngành nghề hoặc cho người dùng tùy chỉnh |
| 4 | _"Hệ thống xử lý CV đa ngôn ngữ thế nào?"_ | AI Agent được prompt để trích xuất theo ngôn ngữ gốc của CV, tránh nhiễu do dịch thuật. Mô hình Sentence-BERT multilingual hỗ trợ cả Tiếng Việt và Tiếng Anh |
| 5 | _"Nếu có 100.000 job thì hiệu suất thế nào?"_ | Stage 1 dùng pgvector index (IVFFlat/HNSW) để approximate nearest neighbor search — O(log n) thay vì O(n). Stage 2 chỉ rerank 200 kết quả nên rất nhanh |
| 6 | _"Tại sao dùng DeepSeek mà không phải GPT-4?"_ | DeepSeek có chi phí thấp hơn đáng kể, hiệu suất trích xuất structured data tương đương, và hỗ trợ OpenAI-compatible API nên dễ thay thế |
| 7 | _"Phần vectorize JD được thực hiện khi nào?"_ | Khi Nhà tuyển dụng tạo hoặc cập nhật tin tuyển dụng, hệ thống tự động gọi AI Agent để vectorize JD tương ứng |

---

## Thứ Tự Tab Trình Duyệt (mở sẵn)

1. **Tab 1:** Trang chủ (Home)
2. **Tab 2:** Trang đăng nhập (đã điền sẵn tài khoản test)
3. **Tab 3:** Trang tìm kiếm công việc (lọc truyền thống)
4. **Tab 4:** Trang quản lý CV (`/infor/cv`)
5. **Tab 5:** Trang gợi ý việc làm (`/infor/recommended-jobs`) — **Tab quan trọng nhất**
6. **Tab 6:** (Tùy chọn) Admin Dashboard
