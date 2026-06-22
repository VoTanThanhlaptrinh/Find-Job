package com.nlu.shared.infrastructure.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.domain.vo.PhoneNumber;
import com.nlu.identity.domain.vo.RoleConstants;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.recruitment.domain.repository.AddressRepository;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.recruitment.domain.vo.ExperienceYears;
import com.nlu.recruitment.domain.vo.SocialLink;
import com.nlu.recruitment.api.dto.JdDataContext;
import com.nlu.recruitment.api.dto.JobJsonDto;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;
import com.nlu.recruitment.domain.model.Address;
import com.nlu.recruitment.domain.model.Category;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.recruitment.domain.repository.CategoryRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.shared.application.HtmlParserService;
import com.nlu.shared.domain.model.EntityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MasterDataSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "Password123!";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@joblist.local";
    private static final String DEFAULT_ADMIN_NAME = "System Admin";

    private static final List<String> COMPANY_NAMES = List.of(
            "TechNova", "BrightCloud", "FinStack", "GreenByte", "UrbanLab", "DataSpring"
    );

    private static final List<String> CATEGORY_NAMES = List.of(
            "Công nghệ thông tin", "Marketing & Truyền thông", "Kế toán & Tài chính", 
            "Bán hàng & Phát triển kinh doanh", "Thiết kế & Mỹ thuật", "Nhân sự & Hành chính", 
            "Dịch vụ Khách hàng", "Kỹ thuật Cơ khí & Xây dựng", "Giáo dục & Đào tạo", "Y tế & Dược phẩm"
    );

    private static final List<String> COMPANY_DESC = List.of(
            "Product-focused team building scalable platforms.",
            "Cloud-first company serving regional enterprises.",
            "Fintech startup focused on secure payments.",
            "Clean-tech org working on energy analytics.",
            "Consumer app studio with fast iteration cycles.",
            "Data engineering consultancy for SMEs."
    );

    // Dữ liệu Logo giả lập (bạn có thể thay bằng link S3/Cloudinary của bạn)
    private static final List<String> COMPANY_LOGOS = List.of(
            "https://ui-avatars.com/api/?name=TechNova&background=0D8ABC&color=fff",
            "https://ui-avatars.com/api/?name=BrightCloud&background=F59E0B&color=fff",
            "https://ui-avatars.com/api/?name=FinStack&background=10B981&color=fff",
            "https://ui-avatars.com/api/?name=GreenByte&background=3B82F6&color=fff",
            "https://ui-avatars.com/api/?name=UrbanLab&background=8B5CF6&color=fff",
            "https://ui-avatars.com/api/?name=DataSpring&background=EF4444&color=fff"
    );

    // Tập trung vào các thành phố lớn tại Việt Nam
    private static final List<AddressSeed> ADDRESS_SEEDS = List.of(
            new AddressSeed("Trụ sở Hà Nội", "Hà Nội", "Duy Tân"),
            new AddressSeed("Văn phòng Nam Từ Liêm", "Hà Nội", "Phạm Hùng"),
            new AddressSeed("Chi nhánh Đống Đa", "Hà Nội", "Tôn Đức Thắng"),
            new AddressSeed("Trụ sở chính HCM", "Hồ Chí Minh", "Nguyễn Huệ"),
            new AddressSeed("Văn phòng Bình Thạnh", "Hồ Chí Minh", "Điện Biên Phủ"),
            new AddressSeed("Chi nhánh Thủ Đức", "Hồ Chí Minh", "Xa lộ Hà Nội"),
            new AddressSeed("Văn phòng Đà Nẵng", "Đà Nẵng", "Bạch Đằng"),
            new AddressSeed("Chi nhánh Thanh Khê", "Đà Nẵng", "Nguyễn Tất Thành"),
            new AddressSeed("Văn phòng Hải Phòng", "Hải Phòng", "Lê Hồng Phong"),
            new AddressSeed("Chi nhánh Cần Thơ", "Cần Thơ", "30 Tháng 4")
    );

    private final RecruitmentRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;
    private final VectorizationClient vectorizationClient;
    private final HtmlParserService htmlParserService;
    private final Random random = new Random();
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Đang kiểm tra và khởi tạo Master Data (Hirer, Address)...");

        seedAdminIfNeeded();

        List<Recruitment> recruitments = new ArrayList<>();
        recruitmentRepository.findAll().forEach(recruitments::add);

        if (recruitments.isEmpty()) {
            log.info("Chưa có dữ liệu Hirer, tiến hành tạo mới...");
            recruitments = seedHirers();
            ensureAddresses(recruitments);
        }
        
        List<Category> categories = seedCategoriesIfNeeded();

        if (jobRepository.count() == 0) {
            log.info("Tiến hành đọc file JSON và tạo dữ liệu Job...");
            var jobsJson = convertJsonFileToList("C:/Users/DELL/Downloads/data.json");
            List<Recruitment> finalRecruitments = recruitments;
            List<Job> jobEntities = jobsJson.stream()
                    .map(dto -> convertJob(dto, finalRecruitments, categories))
                    .toList();

            jobRepository.saveAll(jobEntities);
            log.info("✅ Đã lưu {} công việc vào database.", jobEntities.size());

            // Vectorize tất cả các JD đã lưu
            vectorizeJobs(jobEntities);
        }
        log.info("✅ Hoàn tất khởi tạo Master Data.");
    }

    private void seedAdminIfNeeded() {
        long adminCount = userRepository.countByRoleIn(List.of(RoleConstants.ADMIN, RoleConstants.ROLE_ADMIN));
        if (adminCount > 0) {
            log.info("Existing admin count: {}. Skip admin seeding.", adminCount);
            return;
        }

        User admin = new User();
        admin.setFullName(DEFAULT_ADMIN_NAME);
        admin.setEmail(new EmailAddress(uniqueEmail(DEFAULT_ADMIN_EMAIL)));
        admin.setPassword(new Password(passwordEncoder.encode(DEFAULT_PASSWORD)));
        admin.setRole(RoleConstants.ROLE_ADMIN);
        admin.setAddress("Ho Chi Minh City");
        admin.setMobile(new PhoneNumber("0900000001"));
        admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
        admin.setAccountLocked(false);
        admin.setEnabled(true);
        admin.setActive(true);
        admin.setOauth2Enabled(false);
        admin.setRecordStatus(EntityStatus.ACTIVE);
        userRepository.save(admin);

        log.info("Seeded admin account: {}", admin.getEmail());
    }

    private List<Category> seedCategoriesIfNeeded() {
        List<Category> existingCategories = categoryRepository.findAll();
        if (existingCategories.size() >= CATEGORY_NAMES.size()) {
            existingCategories.sort(java.util.Comparator.comparing(Category::getId));
            return existingCategories;
        }

        log.info("Đang tạo 10 Category...");
        List<Category> newCategories = new ArrayList<>();
        for (String name : CATEGORY_NAMES) {
            Category category = new Category();
            category.setName(name);
            newCategories.add(category);
        }
        categoryRepository.saveAll(newCategories);
        log.info("✅ Đã tạo thành công 10 Category.");
        
        List<Category> allCategories = categoryRepository.findAll();
        allCategories.sort(java.util.Comparator.comparing(Category::getId));
        return allCategories;
    }

    private List<Recruitment> seedHirers() {
        List<Recruitment> recruitments = new ArrayList<>();

        for (int i = 0; i < COMPANY_NAMES.size(); i++) {
            String baseEmail = "hirer" + (i + 1) + "@joblist.local";
            String email = uniqueEmail(baseEmail);

            // Tạo User cho Nhà tuyển dụng
            User user = new User();
            user.setFullName("Hirer " + (i + 1));
            user.setEmail(new EmailAddress(email));
            user.setPassword(new Password(passwordEncoder.encode(DEFAULT_PASSWORD)));
            user.setRole(RoleConstants.ROLE_HIRER);
            user.setAddress(ADDRESS_SEEDS.get(i % ADDRESS_SEEDS.size()).city());
            user.setMobile(new PhoneNumber("09000000" + (10 + i)));
            user.setDateOfBirth(LocalDate.of(1990, 1, 1).plusDays(i * 120L));
            user.setAccountLocked(false);
            user.setEnabled(true);
            user.setActive(true);
            user.setOauth2Enabled(false);
            user.setRecordStatus(EntityStatus.ACTIVE);
            userRepository.save(user);

            // Tạo thông tin Hirer
            Recruitment recruitment = new Recruitment();
            recruitment.setUser(user);
            recruitment.setCompanyName(COMPANY_NAMES.get(i));
            recruitment.setDescription(COMPANY_DESC.get(i));
            recruitment.setSocialLink(new SocialLink("https://company.example/" + COMPANY_NAMES.get(i).toLowerCase()));

            recruitments.add(recruitmentRepository.save(recruitment));
        }

        return recruitments;
    }

    private List<Address> ensureAddresses(List<Recruitment> recruitments) {
        List<Address> existingAddresses = addressRepository.findAll();
        if (!existingAddresses.isEmpty()) {
            return existingAddresses;
        }

        log.info("Chưa có dữ liệu Address, tiến hành tạo mới...");
        return seedAddresses(recruitments);
    }

    private List<Address> seedAddresses(List<Recruitment> recruitments) {
        List<Address> addresses = new ArrayList<>();
        List<List<Address>> hirerAddresses = new ArrayList<>();

        for (int i = 0; i < recruitments.size(); i++) {
            List<Address> ownedAddresses = new ArrayList<>();
            // Mỗi công ty sẽ có 2 địa chỉ
            for (int j = 0; j < 2; j++) {
                int addressIndex = (i * 2 + j) % ADDRESS_SEEDS.size();
                AddressSeed template = ADDRESS_SEEDS.get(addressIndex);
                LocalDateTime createdAt = LocalDateTime.now().minusDays(20L + addressIndex);

                Address address = new Address();
                address.setLocationName(template.locationName());
                address.setCity(template.city());
                address.setStreet(template.street());
                address.setIsDefault(j == 0);

                ownedAddresses.add(address);
                addresses.add(address);
            }
            hirerAddresses.add(ownedAddresses);
        }

        addressRepository.saveAll(addresses);

        // Map địa chỉ vào Hirer và User
        for (int i = 0; i < recruitments.size(); i++) {
            Recruitment recruitment = recruitments.get(i);
            List<Address> ownedAddresses = hirerAddresses.get(i);
            recruitment.setAddresses(ownedAddresses);
            recruitments.set(i, recruitmentRepository.save(recruitment));

            User user = recruitment.getUser();
            if (user != null && !ownedAddresses.isEmpty()) {
                user.setAddress(formatAddress(ownedAddresses.get(0)));
                userRepository.save(user);
            }
        }

        return addresses;
    }

    private String formatAddress(Address address) {
        return address.getStreet() + ", " + address.getCity();
    }

    private String uniqueEmail(String baseEmail) {
        String email = baseEmail;
        int counter = 1;
        while (userRepository.findByEmail_Value(email).isPresent()) {
            int at = baseEmail.indexOf('@');
            String prefix = at >= 0 ? baseEmail.substring(0, at) : baseEmail;
            String domain = at >= 0 ? baseEmail.substring(at) : "@joblist.local";
            email = prefix + counter + domain;
            counter++;
        }
        return email;
    }

    private record AddressSeed(String locationName, String city, String street) {}
    public List<JobJsonDto> convertJsonFileToList(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                System.err.println("File không tồn tại!");
                return new ArrayList<>();
            }

            // Sử dụng TypeReference để định nghĩa List<JobDTO>
            return objectMapper.readValue(file, new TypeReference<List<JobJsonDto>>() {});

        } catch (IOException e) {
            System.err.println("Lỗi khi convert danh sách JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    private Job convertJob(JobJsonDto j, List<Recruitment> recruitments, List<Category> categories) {
        Job job = new Job();
        job.setTitle(j.getTitle());
        job.setDescription(j.getJobDescription());
        job.setRequireDetails(j.getJobRequirement());
        job.setSkill(j.getJobSkill());
        job.setMoreDetail(j.getMoreDetail());
        job.setYearOfExperience(new ExperienceYears(j.getYearOfExperience()));
        job.setTime(j.getTime());

        // 1. Set Hirer (Lấy ngẫu nhiên hoặc theo ID giả lập từ JSON)
        Recruitment randomRecruitment = recruitments.get(random.nextInt(recruitments.size()));
        job.setRecruitment(randomRecruitment);

        // 2. Set Address (Lấy một địa chỉ trong danh sách địa chỉ của Hirer đó)
        if (randomRecruitment.getAddresses() != null && !randomRecruitment.getAddresses().isEmpty()) {
            job.setAddress(randomRecruitment.getAddresses().get(0));
        }

        // 3. Set Expired Date (Ngẫu nhiên từ 15 đến 60 ngày tới)
        job.setExpiredDate(LocalDateTime.now().plusDays(15 + random.nextInt(45)));

        // 4. Set Photo (Lấy logo của Hirer làm ảnh đại diện Job luôn cho đồng bộ)
        // Giả sử Hirer có trường logo, hoặc bạn dùng COMPANY_LOGOS dựa trên index
        job.setLogo(COMPANY_LOGOS.get(random.nextInt(COMPANY_LOGOS.size())));

        // 5. Set Salary (Xử lý 3 dạng: VNĐ, Đô, Thỏa thuận)
        job.setSalary(generateRandomSalary());

        // Set ngày tạo
        // (Bỏ qua vì JPA Auditing tự động lo)

        // 6. Set Headcount (Số lượng tuyển: 1 - 10)
        job.setHeadcount(1 + random.nextInt(10));

        // 7. Set Category
        if (j.getCategory() != null && j.getCategory() >= 1 && j.getCategory() <= categories.size()) {
            job.setCategory(categories.get(j.getCategory() - 1));
        }

        return job;
    }
    private String generateRandomSalary() {
        int type = random.nextInt(3); // 0, 1, 2

        return switch (type) {
            case 0 -> {
                // Dạng VNĐ: 10 - 15 triệu
                int min = 8 + random.nextInt(7);
                int max = min + 3 + random.nextInt(10);
                yield min + " - " + max + " triệu";
            }
            case 1 -> {
                // Dạng Đô: $500 - $1000
                int min = 500 + (random.nextInt(10) * 100);
                int max = min + 500 + (random.nextInt(10) * 100);
                yield "$" + min + " - $" + max;
            }
            default -> "Thỏa thuận";
        };
    }

    /**
     * Vectorize danh sách các Job đã lưu
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private void vectorizeJobs(List<Job> jobs) {
        log.info("Bắt đầu vectorize {} công việc...", jobs.size());

        // We must use AtomicInteger because standard ints are not thread-safe
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Map each job to an asynchronous task
        List<CompletableFuture<Void>> futures = jobs.stream()
                .map(job -> CompletableFuture.runAsync(() -> {
                    try {
                        VectorizeJdRequest request = createVectorizeJdRequest(job);
                        vectorizationClient.vectorizeJd(request); // This still has your MDC logic, which is great!
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        log.warn("Không thể vectorize job ID {}: {}", job.getId(), e.getMessage());
                    }
                }, executor)) // Pass the custom thread pool here
                .toList();

        // Block the main thread here until all 800 background tasks are finished
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("✅ Hoàn tất vectorize: {} thành công, {} thất bại", successCount.get(), failCount.get());
    }

    /**
     * Tạo VectorizeJdRequest từ Job entity
     */
    private VectorizeJdRequest createVectorizeJdRequest(Job job) {
        VectorizeJdRequest request = new VectorizeJdRequest();
        request.setJobId(job.getId());
        request.setUserId(job.getRecruitment().getId());
        request.setRequiredYearsExperience(job.getYearOfExperience());
        request.setDeadlineDate(LocalDate.from(job.getExpiredDate()));

        // Tạo JdDataContext với text đã được trích xuất từ HTML
        JdDataContext dataContext = new JdDataContext();
        dataContext.setSkillsAndProjectsContext(extractSkillsContext(job));
        dataContext.setExperienceContext(extractExperienceContext(job));
        request.setData(dataContext);

        return request;
    }

    /**
     * Trích xuất context về skills từ Job
     */
    private String extractSkillsContext(Job job) {
        StringBuilder sb = new StringBuilder();

        if (job.getSkill() != null && !job.getSkill().isBlank()) {
            sb.append(htmlParserService.parseHtml(job.getSkill()));
        }

        if (job.getMoreDetail() != null && !job.getMoreDetail().isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(htmlParserService.parseHtml(job.getMoreDetail()));
        }

        return sb.toString();
    }

    /**
     * Trích xuất context về experience/requirements từ Job
     */
    private String extractExperienceContext(Job job) {
        StringBuilder sb = new StringBuilder();

        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            sb.append(htmlParserService.parseHtml(job.getDescription()));
        }

        if (job.getRequireDetails() != null && !job.getRequireDetails().isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(htmlParserService.parseHtml(job.getRequireDetails()));
        }

        return sb.toString();
    }
}
