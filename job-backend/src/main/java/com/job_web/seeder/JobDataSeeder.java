package com.job_web.seeder;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import lombok.RequiredArgsConstructor;
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

@Component
@Profile("dev")
@RequiredArgsConstructor
public class JobDataSeeder implements CommandLineRunner {
    private static final int JOB_COUNT = 50;
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
    private static final List<AddressSeed> ADDRESS_SEEDS = List.of(
            new AddressSeed("Ha Noi", "Cau Giay", "Duy Tan"),
            new AddressSeed("Ha Noi", "Nam Tu Liem", "Pham Hung"),
            new AddressSeed("Ho Chi Minh", "Quan 1", "Nguyen Hue"),
            new AddressSeed("Ho Chi Minh", "Binh Thanh", "Dien Bien Phu"),
            new AddressSeed("Da Nang", "Hai Chau", "Bach Dang"),
            new AddressSeed("Da Nang", "Thanh Khe", "Nguyen Tat Thanh"),
            new AddressSeed("Hai Phong", "Ngo Quyen", "Le Hong Phong"),
            new AddressSeed("Hai Phong", "Hong Bang", "Tran Hung Dao"),
            new AddressSeed("Can Tho", "Ninh Kieu", "30 Thang 4"),
            new AddressSeed("Can Tho", "Cai Rang", "Vo Nguyen Giap"),
            new AddressSeed("Ha Noi", "Dong Da", "Ton Duc Thang"),
            new AddressSeed("Ho Chi Minh", "Thu Duc", "Xa lo Ha Noi")
    );
    private static final List<String> JOB_TITLES = List.of(
            "Java Backend Developer",
            "Frontend React Engineer",
            "Fullstack Engineer",
            "QA Automation Engineer",
            "DevOps Engineer",
            "Data Analyst",
            "Mobile Flutter Developer",
            "UI/UX Designer",
            "Node.js Developer",
            "Product Manager",
            "Business Analyst",
            "Data Engineer"
    );
    private static final List<String> JOB_TIMES = List.of(
            "Full time", "Part time", "Hybrid", "Remote"
    );
    private static final List<String> SKILLS = List.of(
            "Java, Spring Boot, MySQL",
            "React, TypeScript, HTML, CSS",
            "Node.js, Express, PostgreSQL",
            "Docker, Kubernetes, CI/CD",
            "Python, SQL, Power BI",
            "Flutter, Dart, REST API",
            "Figma, UX Research, Prototyping",
            "AWS, Terraform, Linux"
    );
    private static final List<String> REQUIREMENTS = List.of(
            "2+ years experience, solid problem solving.",
            "Strong teamwork and communication skills.",
            "Able to design clean APIs and data models.",
            "Experience with testing and code review.",
            "Comfortable with agile delivery."
    );

    private final JobRepository jobRepository;
    private final HirerRepository hirerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        List<Hirer> hirers = new ArrayList<>();
        hirerRepository.findAll().forEach(hirers::add);

        if (hirers.isEmpty()) {
            hirers = seedHirers();
        }

        List<Address> addresses = ensureAddresses(hirers);

        if (jobRepository.count() == 0) {
            seedJobs(hirers, addresses);
        }
    }

    private List<Hirer> seedHirers() {
        List<Hirer> hirers = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < COMPANY_NAMES.size(); i++) {
            String baseEmail = "hirer" + (i + 1) + "@joblist.local";
            String email = uniqueEmail(baseEmail);

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

            Hirer hirer = new Hirer();
            hirer.setUser(user);
            hirer.setCompanyName(COMPANY_NAMES.get(i));
            hirer.setDescription(COMPANY_DESC.get(i));
            hirer.setSocialLink("https://company.example/" + COMPANY_NAMES.get(i).toLowerCase());
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

        return seedAddresses(hirers);
    }

    private List<Address> seedAddresses(List<Hirer> hirers) {
        List<Address> addresses = new ArrayList<>();
        List<List<Address>> hirerAddresses = new ArrayList<>();
        long nextId = 1L;

        for (int i = 0; i < hirers.size(); i++) {
            List<Address> ownedAddresses = new ArrayList<>();
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

    private void seedJobs(List<Hirer> hirers, List<Address> addresses) {
        List<Job> jobs = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < JOB_COUNT; i++) {
            Hirer hirer = hirers.get(i % hirers.size());
            String title = JOB_TITLES.get(i % JOB_TITLES.size()) + " (" + hirer.getCompanyName() + ")";
            Address address = pickAddressForHirer(hirer, addresses, i);

            Job job = new Job();
            job.setTitle(title);
            job.setSalary(5000000 + (i % 10) * 1500000);
            job.setTime(JOB_TIMES.get(i % JOB_TIMES.size()));
            job.setAddress(address);
            job.setSkill(SKILLS.get(i % SKILLS.size()));
            job.setRequireDetails("Requirements: " + REQUIREMENTS.get(i % REQUIREMENTS.size()));
            job.setDescription("Work with the " + hirer.getCompanyName()
                    + " team to deliver high-quality features and stable services.");
            job.setCreateDate(now.minus(Duration.ofDays(45 + i)));
            job.setModifiedDate(now.minus(Duration.ofDays(10 + (i % 7))));
            job.setExpiredDate(now.plus(Duration.ofDays(30 + (i % 60))));
            job.setHirer(hirer);
            jobs.add(job);
        }

        jobRepository.saveAll(jobs);
    }

    private Address pickAddressForHirer(Hirer hirer, List<Address> addresses, int index) {
        List<Address> ownedAddresses = hirer.getAddresses();
        if (ownedAddresses != null && !ownedAddresses.isEmpty()) {
            return ownedAddresses.get(index % ownedAddresses.size());
        }
        return addresses.isEmpty() ? null : addresses.get(index % addresses.size());
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
