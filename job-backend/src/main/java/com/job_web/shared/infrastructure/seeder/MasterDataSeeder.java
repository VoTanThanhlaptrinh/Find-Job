package com.job_web.shared.infrastructure.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.identity.domain.vo.EmailAddress;
import com.job_web.identity.domain.vo.Password;
import com.job_web.identity.domain.vo.PhoneNumber;
import com.job_web.identity.domain.vo.RoleConstants;
import com.job_web.recruiment.domain.repository.AddressRepository;
import com.job_web.recruiment.domain.repository.RecruitmentRepository;
import com.job_web.recruiment.domain.repository.JobRepository;
import com.job_web.identity.domain.repository.UserRepository;
import com.job_web.recruiment.domain.vo.ExperienceYears;
import com.job_web.recruiment.domain.vo.SocialLink;
import com.job_web.recruiment.api.dto.JdDataContext;
import com.job_web.recruiment.api.dto.JobDTOJson;
import com.job_web.recruiment.api.dto.VectorizeJdRequest;
import com.job_web.recruiment.domain.model.Address;
import com.job_web.recruiment.domain.model.Recruiment;
import com.job_web.recruiment.domain.model.Job;
import com.job_web.identity.domain.model.User;
import com.job_web.application_process.application.ApiService;
import com.job_web.shared.application.HtmlParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
            new AddressSeed("Hà Nội", "Cầu Giấy", "Duy Tân"),
            new AddressSeed("Hà Nội", "Nam Từ Liêm", "Phạm Hùng"),
            new AddressSeed("Hà Nội", "Đống Đa", "Tôn Đức Thắng"),
            new AddressSeed("Hồ Chí Minh", "Quận 1", "Nguyễn Huệ"),
            new AddressSeed("Hồ Chí Minh", "Bình Thạnh", "Điện Biên Phủ"),
            new AddressSeed("Hồ Chí Minh", "Thủ Đức", "Xa lộ Hà Nội"),
            new AddressSeed("Đà Nẵng", "Hải Châu", "Bạch Đằng"),
            new AddressSeed("Đà Nẵng", "Thanh Khê", "Nguyễn Tất Thành"),
            new AddressSeed("Hải Phòng", "Ngô Quyền", "Lê Hồng Phong"),
            new AddressSeed("Cần Thơ", "Ninh Kiều", "30 Tháng 4")
    );

    private final RecruitmentRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;
    private final ApiService apiService;
    private final HtmlParserService htmlParserService;
    private final Random random = new Random();
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Đang kiểm tra và khởi tạo Master Data (Hirer, Address)...");

        seedAdminIfNeeded();

        List<Recruiment> recruiments = new ArrayList<>();
        recruitmentRepository.findAll().forEach(recruiments::add);

        if (recruiments.isEmpty()) {
            log.info("Chưa có dữ liệu Hirer, tiến hành tạo mới...");
            recruiments = seedHirers();
            ensureAddresses(recruiments);
        }
        if (jobRepository.count() == 0) {
            log.info("Tiến hành đọc file JSON và tạo dữ liệu Job...");
            var jobsJson = convertJsonFileToList("C:/Users/DELL/Downloads/job_data_500.json");
            jobsJson.addAll(convertJsonFileToList("C:/Users/DELL/Downloads/job_data_vn_300.json"));
            List<Recruiment> finalRecruiments = recruiments;
            List<Job> jobEntities = jobsJson.stream()
                    .map(dto -> convertJob(dto, finalRecruiments))
                    .toList();

            jobRepository.saveAll(jobEntities);
            log.info("✅ Đã lưu {} công việc vào database.", jobEntities.size());

            // Vectorize tất cả các JD đã lưu
//            vectorizeJobs(jobEntities);
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
        admin.setCreateDate(LocalDateTime.now());
        userRepository.save(admin);

        log.info("Seeded admin account: {}", admin.getEmail());
    }

    private List<Recruiment> seedHirers() {
        List<Recruiment> recruiments = new ArrayList<>();

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
            user.setCreateDate(LocalDateTime.now());
            userRepository.save(user);

            // Tạo thông tin Hirer
            Recruiment recruiment = new Recruiment();
            recruiment.setUser(user);
            recruiment.setCompanyName(COMPANY_NAMES.get(i));
            recruiment.setDescription(COMPANY_DESC.get(i));
            recruiment.setSocialLink(new SocialLink("https://company.example/" + COMPANY_NAMES.get(i).toLowerCase()));

            recruiment.setCreateDate(LocalDateTime.now());
            recruiment.setModifiedDate(LocalDateTime.now());
            recruiments.add(recruitmentRepository.save(recruiment));
        }

        return recruiments;
    }

    private List<Address> ensureAddresses(List<Recruiment> recruiments) {
        List<Address> existingAddresses = addressRepository.findAll();
        if (!existingAddresses.isEmpty()) {
            return existingAddresses;
        }

        log.info("Chưa có dữ liệu Address, tiến hành tạo mới...");
        return seedAddresses(recruiments);
    }

    private List<Address> seedAddresses(List<Recruiment> recruiments) {
        List<Address> addresses = new ArrayList<>();
        List<List<Address>> hirerAddresses = new ArrayList<>();

        for (int i = 0; i < recruiments.size(); i++) {
            List<Address> ownedAddresses = new ArrayList<>();
            // Mỗi công ty sẽ có 2 địa chỉ
            for (int j = 0; j < 2; j++) {
                int addressIndex = (i * 2 + j) % ADDRESS_SEEDS.size();
                AddressSeed template = ADDRESS_SEEDS.get(addressIndex);
                LocalDateTime createdAt = LocalDateTime.now().minusDays(20L + addressIndex);

                Address address = new Address();
                address.setCity(template.city());
                address.setStreet(template.street());
                address.setCreateDate(createdAt);
                address.setUpdateDate(createdAt.plusDays(1));

                ownedAddresses.add(address);
                addresses.add(address);
            }
            hirerAddresses.add(ownedAddresses);
        }

        addressRepository.saveAll(addresses);

        // Map địa chỉ vào Hirer và User
        for (int i = 0; i < recruiments.size(); i++) {
            Recruiment recruiment = recruiments.get(i);
            List<Address> ownedAddresses = hirerAddresses.get(i);
            recruiment.setAddresses(ownedAddresses);
            recruiments.set(i, recruitmentRepository.save(recruiment));

            User user = recruiment.getUser();
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
        while (userRepository.findByEmail(email).isPresent()) {
            int at = baseEmail.indexOf('@');
            String prefix = at >= 0 ? baseEmail.substring(0, at) : baseEmail;
            String domain = at >= 0 ? baseEmail.substring(at) : "@joblist.local";
            email = prefix + "+" + counter + domain;
            counter++;
        }
        return email;
    }

    private record AddressSeed(String city, String street) {
        private AddressSeed(String city, String ignoredDistrict, String street) {
            this(city, street);
        }
    }
    public List<JobDTOJson> convertJsonFileToList(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                System.err.println("File không tồn tại!");
                return new ArrayList<>();
            }

            // Sử dụng TypeReference để định nghĩa List<JobDTO>
            return objectMapper.readValue(file, new TypeReference<List<JobDTOJson>>() {});

        } catch (IOException e) {
            System.err.println("Lỗi khi convert danh sách JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    private Job convertJob(JobDTOJson j, List<Recruiment> recruiments) {
        Job job = new Job();
        job.setTitle(j.getTitle());
        job.setDescription(j.getJobDescription());
        job.setRequireDetails(j.getJobRequirement());
        job.setSkill(j.getJobSkill());
        job.setMoreDetail(j.getMoreDetail());
        job.setYearOfExperience(new ExperienceYears(j.getYearOfExperience()));
        job.setTime(j.getTime());

        // 1. Set Hirer (Lấy ngẫu nhiên hoặc theo ID giả lập từ JSON)
        Recruiment randomRecruiment = recruiments.get(random.nextInt(recruiments.size()));
        job.setRecruiment(randomRecruiment);

        // 2. Set Address (Lấy một địa chỉ trong danh sách địa chỉ của Hirer đó)
        if (randomRecruiment.getAddresses() != null && !randomRecruiment.getAddresses().isEmpty()) {
            job.setAddress(randomRecruiment.getAddresses().get(0));
        }

        // 3. Set Expired Date (Ngẫu nhiên từ 15 đến 60 ngày tới)
        job.setExpiredDate(LocalDateTime.now().plusDays(15 + random.nextInt(45)));

        // 4. Set Photo (Lấy logo của Hirer làm ảnh đại diện Job luôn cho đồng bộ)
        // Giả sử Hirer có trường logo, hoặc bạn dùng COMPANY_LOGOS dựa trên index
        job.setLogo(COMPANY_LOGOS.get(random.nextInt(COMPANY_LOGOS.size())));

        // 5. Set Salary (Xử lý 3 dạng: VNĐ, Đô, Thỏa thuận)
        job.setSalary(generateRandomSalary());

        // Set ngày tạo
        job.setCreateDate(LocalDateTime.now());

        // 6. Set Headcount (Số lượng tuyển: 1 - 10)
        job.setHeadcount(1 + random.nextInt(10));

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
    private void vectorizeJobs(List<Job> jobs) {
        log.info("Bắt đầu vectorize {} công việc...", jobs.size());
        int successCount = 0;
        int failCount = 0;

        for (Job job : jobs) {
            try {
                VectorizeJdRequest request = createVectorizeJdRequest(job);
                apiService.vectorizeJd(request);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("Không thể vectorize job ID {}: {}", job.getId(), e.getMessage());
            }
        }

        log.info("✅ Hoàn tất vectorize: {} thành công, {} thất bại", successCount, failCount);
    }

    /**
     * Tạo VectorizeJdRequest từ Job entity
     */
    private VectorizeJdRequest createVectorizeJdRequest(Job job) {
        VectorizeJdRequest request = new VectorizeJdRequest();
        request.setJobId(job.getId());
        request.setUserId(job.getRecruiment().getId());
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
