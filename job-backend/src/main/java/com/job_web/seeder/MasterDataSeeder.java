package com.job_web.seeder;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Đang kiểm tra và khởi tạo Master Data (Hirer, Address)...");

        List<Hirer> hirers = new ArrayList<>();
        hirerRepository.findAll().forEach(hirers::add);

        if (hirers.isEmpty()) {
            log.info("Chưa có dữ liệu Hirer, tiến hành tạo mới...");
            hirers = seedHirers();
        }

        ensureAddresses(hirers);

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
}