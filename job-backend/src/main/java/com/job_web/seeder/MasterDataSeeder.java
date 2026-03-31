package com.job_web.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.job.JdDataContext;
import com.job_web.dto.job.JobDTOJson;
import com.job_web.dto.job.VectorizeJdRequest;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.ai.ApiService;
import com.job_web.service.support.HtmlParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
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

    private final HirerRepository hirerRepository;
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

        List<Hirer> hirers = new ArrayList<>();
        hirerRepository.findAll().forEach(hirers::add);

        if (hirers.isEmpty()) {
            log.info("Chưa có dữ liệu Hirer, tiến hành tạo mới...");
            hirers = seedHirers();
            ensureAddresses(hirers);
        }
        if (jobRepository.count() == 0) {
            log.info("Tiến hành đọc file JSON và tạo dữ liệu Job...");
            var jobsJson = convertJsonFileToList("C:/Users/DELL/Downloads/job_data_500.json");
            jobsJson.addAll(convertJsonFileToList("C:/Users/DELL/Downloads/job_data_vn_300.json"));
            List<Hirer> finalHirers = hirers;
            List<Job> jobEntities = jobsJson.stream()
                    .map(dto -> convertJob(dto, finalHirers))
                    .toList();

            jobRepository.saveAll(jobEntities);
            log.info("✅ Đã lưu {} công việc vào database.", jobEntities.size());

            // Vectorize tất cả các JD đã lưu
//            vectorizeJobs(jobEntities);
        }
        log.info("✅ Hoàn tất khởi tạo Master Data.");
    }

    private List<Hirer> seedHirers() {
        List<Hirer> hirers = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < COMPANY_NAMES.size(); i++) {
            String baseEmail = "hirer" + (i + 1) + "@joblist.local";
            String email = uniqueEmail(baseEmail);

            // Tạo User cho Nhà tuyển dụng
            User user = new User();
            user.setFullName("Hirer " + (i + 1));
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRole("ROLE_HIRER");
            user.setAddress(ADDRESS_SEEDS.get(i % ADDRESS_SEEDS.size()).city());
            user.setMobile("09000000" + (10 + i));
            user.setDateOfBirth(LocalDate.of(1990, 1, 1).plusDays(i * 120L));
            user.setAccountLocked(false);
            user.setEnabled(true);
            user.setActive(true);
            user.setOauth2Enabled(false);
            user.setCreateDate(LocalDateTime.now());
            userRepository.save(user);

            // Tạo thông tin Hirer
            Hirer hirer = new Hirer();
            hirer.setUser(user);
            hirer.setCompanyName(COMPANY_NAMES.get(i));
            hirer.setDescription(COMPANY_DESC.get(i));
            hirer.setSocialLink("https://company.example/" + COMPANY_NAMES.get(i).toLowerCase());

            // Set Logo cho Hirer (hoặc thuộc tính tương đương trong Entity của bạn)
            // hirer.setLogo(COMPANY_LOGOS.get(i));

            hirer.setCreateDate(now.minus(Duration.ofDays(60 + i)));
            hirer.setModifiedDate(now.minus(Duration.ofDays(30 + i)));
            hirers.add(hirerRepository.save(hirer));
        }

        return hirers;
    }

    private List<Address> ensureAddresses(List<Hirer> hirers) {
        List<Address> existingAddresses = addressRepository.findAll();
        if (!existingAddresses.isEmpty()) {
            return existingAddresses;
        }

        log.info("Chưa có dữ liệu Address, tiến hành tạo mới...");
        return seedAddresses(hirers);
    }

    private List<Address> seedAddresses(List<Hirer> hirers) {
        List<Address> addresses = new ArrayList<>();
        List<List<Address>> hirerAddresses = new ArrayList<>();
        long nextId = 1L;

        for (int i = 0; i < hirers.size(); i++) {
            List<Address> ownedAddresses = new ArrayList<>();
            // Mỗi công ty sẽ có 2 địa chỉ
            for (int j = 0; j < 2; j++) {
                int addressIndex = (i * 2 + j) % ADDRESS_SEEDS.size();
                AddressSeed template = ADDRESS_SEEDS.get(addressIndex);
                LocalDateTime createdAt = LocalDateTime.now().minusDays(20L + addressIndex);

                Address address = new Address();
                address.setId(nextId++);
                address.setCity(template.city());
                address.setDistrict(template.district());
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
        for (int i = 0; i < hirers.size(); i++) {
            Hirer hirer = hirers.get(i);
            List<Address> ownedAddresses = hirerAddresses.get(i);
            hirer.setAddresses(ownedAddresses);
            hirers.set(i, hirerRepository.save(hirer));

            User user = hirer.getUser();
            if (user != null && !ownedAddresses.isEmpty()) {
                user.setAddress(formatAddress(ownedAddresses.get(0)));
                userRepository.save(user);
            }
        }

        return addresses;
    }

    private String formatAddress(Address address) {
        return address.getStreet() + ", " + address.getDistrict() + ", " + address.getCity();
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

    private record AddressSeed(String city, String district, String street) {
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
            List<JobDTOJson> jobList = objectMapper.readValue(file, new TypeReference<List<JobDTOJson>>() {});

            return jobList;

        } catch (IOException e) {
            System.err.println("Lỗi khi convert danh sách JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    private Job convertJob(JobDTOJson j, List<Hirer> hirers) {
        Job job = new Job();
        job.setTitle(j.getTitle());
        job.setDescription(j.getJobDescription());
        job.setRequireDetails(j.getJobRequirement());
        job.setSkill(j.getJobSkill());
        job.setMoreDetail(j.getMoreDetail());
        job.setYearOfExperience(j.getYearOfExperience());
        job.setTime(j.getTime());

        // 1. Set Hirer (Lấy ngẫu nhiên hoặc theo ID giả lập từ JSON)
        Hirer randomHirer = hirers.get(random.nextInt(hirers.size()));
        job.setHirer(randomHirer);

        // 2. Set Address (Lấy một địa chỉ trong danh sách địa chỉ của Hirer đó)
        if (randomHirer.getAddresses() != null && !randomHirer.getAddresses().isEmpty()) {
            job.setAddress(randomHirer.getAddresses().get(0));
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
        request.setUserId(job.getHirer().getId());
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