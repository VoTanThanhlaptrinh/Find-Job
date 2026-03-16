package com.job_web.seeder;

import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
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
    private static final List<String> ADDRESSES = List.of(
            "Ha Noi", "Ho Chi Minh", "Da Nang", "Hai Phong", "Can Tho"
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
    private final PasswordEncoder passwordEncoder;

    @Profile("dev")
    @Override
    public void run(String... args) {
        if (jobRepository.count() > 0) {
            return;
        }

        List<Hirer> hirers = new ArrayList<>();
        hirerRepository.findAll().forEach(hirers::add);

        if (hirers.isEmpty()) {
            hirers = seedHirers();
        }

        seedJobs(hirers);
    }

    private List<Hirer> seedHirers() {
        List<Hirer> hirers = new ArrayList<>();
        LocalDateTime userCreatedAt = LocalDateTime.now().minusDays(120);
        Instant now = Instant.now();

        for (int i = 0; i < COMPANY_NAMES.size(); i++) {
            String baseEmail = "hirer" + (i + 1) + "@joblist.local";
            String email = uniqueEmail(baseEmail);

            User user = new User();
            user.setFullName("Hirer " + (i + 1));
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRole("ROLE_HIRER");
            user.setAddress(ADDRESSES.get(i % ADDRESSES.size()));
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

    private void seedJobs(List<Hirer> hirers) {
        List<Job> jobs = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < JOB_COUNT; i++) {
            Hirer hirer = hirers.get(i % hirers.size());
            String title = JOB_TITLES.get(i % JOB_TITLES.size()) + " (" + hirer.getCompanyName() + ")";

            Job job = new Job();
            job.setTitle(title);
            job.setSalary(800 + (i % 10) * 150);
            job.setTime(JOB_TIMES.get(i % JOB_TIMES.size()));
            job.setAddress(ADDRESSES.get(i % ADDRESSES.size()));
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
}
