package com.cirs.config;

import com.cirs.entity.IncidentCategory;
import com.cirs.entity.User;
import com.cirs.enums.Role;
import com.cirs.repository.IncidentCategoryRepository;
import com.cirs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${CIRS_ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${CIRS_INVESTIGATOR_PASSWORD}")
    private String investigatorPassword;

    @Value("${CIRS_USER_PASSWORD}")
    private String userPassword;

    public DataInitializer(UserRepository userRepository,
                           IncidentCategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedCategories();
        seedUsers();
    }

    private void seedCategories() {
        List<String[]> defaultCategories = List.of(
                new String[]{"Phishing", "Deceptive emails, fake portals, or fraudulent messaging aiming to harvest credentials or sensitive data."},
                new String[]{"Financial Fraud", "Unauthorized transactions, wire scams, card skimming, or fraudulent cryptocurrency schemes."},
                new String[]{"Account Compromise", "Unauthorized access or takeover of social media, email, banking, or enterprise accounts."},
                new String[]{"Malware", "Infection by viruses, trojans, spyware, keyloggers, or malicious software payloads."},
                new String[]{"Ransomware", "Malicious software that encrypts user or enterprise data and demands ransom payment."},
                new String[]{"Data Breach", "Unauthorized leakage, exfiltration, or exposure of confidential, personal, or proprietary records."},
                new String[]{"Identity Theft", "Fraudulent use of a person's personal identifying information without authorization."},
                new String[]{"Cyberstalking", "Persistent harassment, intimidation, or tracking of individuals using electronic communications."},
                new String[]{"Social Media Attack", "Impersonation, defamation, malicious bots, or harassment across social networking platforms."},
                new String[]{"Other", "Unclassified cyber security incidents or suspicious digital activities."}
        );

        for (String[] cat : defaultCategories) {
            String name = cat[0];
            String description = cat[1];

            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                categoryRepository.save(new IncidentCategory(name, description));
                logger.info("Seeded incident category: {}", name);
            }
        }
    }

    private void seedUsers() {

        // 1. Admin account
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                    "admin",
                    "admin@cirs.gov",
                    passwordEncoder.encode(adminPassword),
                    "Chief Information Security Officer",
                    "+1-555-0100",
                    Role.ROLE_ADMIN
            );

            userRepository.save(admin);
            logger.info("Seeded default Administrator account: admin");
        }

        // 2. Lead Investigator account
        if (userRepository.findByUsername("investigator1").isEmpty()) {
            User investigator = new User(
                    "investigator1",
                    "lead.investigator@cirs.gov",
                    passwordEncoder.encode(investigatorPassword),
                    "Sarah Connor",
                    "+1-555-0101",
                    Role.ROLE_INVESTIGATOR
            );

            userRepository.save(investigator);
            logger.info("Seeded default Investigator account: investigator1");
        }

        // 3. Regular citizen User account
        if (userRepository.findByUsername("user1").isEmpty()) {
            User citizen = new User(
                    "user1",
                    "alice.smith@example.com",
                    passwordEncoder.encode(userPassword),
                    "Alice Smith",
                    "+1-555-0102",
                    Role.ROLE_USER
            );

            userRepository.save(citizen);
            logger.info("Seeded default Citizen account: user1");
        }
    }
}