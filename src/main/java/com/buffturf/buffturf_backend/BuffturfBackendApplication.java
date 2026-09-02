package com.buffturf.buffturf_backend;

import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class BuffturfBackendApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(BuffturfBackendApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		System.out.println("✅ Application TimeZone set to: " + TimeZone.getDefault().getID());
	}

	private static void loadEnv() {
		java.io.File envFile = new java.io.File(".env");
		if (!envFile.exists()) {
			envFile = new java.io.File("../.env");
		}
		if (envFile.exists()) {
			try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					int eqIdx = line.indexOf('=');
					if (eqIdx > 0) {
						String key = line.substring(0, eqIdx).trim();
						String value = line.substring(eqIdx + 1).trim();
						if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
							value = value.substring(1, value.length() - 1);
						} else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
							value = value.substring(1, value.length() - 1);
						}
						System.setProperty(key, value);
					}
				}
				System.out.println("✅ Loaded environment variables from " + envFile.getAbsolutePath());
			} catch (Exception e) {
				System.err.println("❌ Failed to load .env file: " + e.getMessage());
			}
		} else {
			System.out.println("⚠️ No .env file found at " + envFile.getAbsolutePath());
		}
	}

	@Bean
	CommandLineRunner createAdmin(UserRepository userRepository,
								  com.buffturf.buffturf_backend.repository.TurfRepository turfRepository,
								  PasswordEncoder passwordEncoder,
								  org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// Safely modify legacy columns
				jdbcTemplate.execute("ALTER TABLE bookings MODIFY COLUMN slot_id bigint NULL");
				jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL");
				System.out.println("✅ Successfully updated schema constraints!");
			} catch (Exception e) {
				System.out.println("ℹ️ Database schema is already clean or modification failed. Details: " + e.getMessage());
			}
			
			// 1. Seed Super Admin
			if (!userRepository.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@buffturf.com");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole(User.Role.ADMIN);
				admin.setPhoneNumber("9999999999");
				userRepository.save(admin);
				System.out.println("✅ Super Admin created: admin@buffturf.com / admin123");
			} else {
				System.out.println("✅ Admin already exists!");
			}

			// 2. Seed Default Turf Owners for Multi-Tenant Testing
			java.util.List<com.buffturf.buffturf_backend.model.Turf> turfs = turfRepository.findAll();
			for (com.buffturf.buffturf_backend.model.Turf turf : turfs) {
				if (turf.getOwner() == null) {
					String safeName = turf.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
					if (safeName.isEmpty()) safeName = "turf" + turf.getId();
					String ownerUsername = "owner_" + safeName;
					String ownerEmail = "owner." + safeName + "@buffturf.com";

					User owner = userRepository.findByEmail(ownerEmail).orElse(null);
					if (owner == null) {
						owner = new User();
						owner.setUsername(ownerUsername);
						owner.setEmail(ownerEmail);
						owner.setPassword(passwordEncoder.encode("owner123"));
						owner.setRole(User.Role.TURF_OWNER);
						owner.setPhoneNumber("9888888888");
						owner = userRepository.save(owner);
						System.out.println("✅ Seeded Turf Owner: " + ownerEmail + " (Password: owner123) for Turf: " + turf.getName());
					}
					turf.setOwner(owner);
					turfRepository.save(turf);
				}
			}

			// Also ensure a standard demo owner "owner@buffturf.com" exists
			if (!userRepository.existsByEmail("owner@buffturf.com")) {
				User demoOwner = new User();
				demoOwner.setUsername("turf_owner");
				demoOwner.setEmail("owner@buffturf.com");
				demoOwner.setPassword(passwordEncoder.encode("owner123"));
				demoOwner.setRole(User.Role.TURF_OWNER);
				demoOwner.setPhoneNumber("9876543210");
				demoOwner = userRepository.save(demoOwner);
				if (!turfs.isEmpty() && turfs.get(0).getOwner() == null) {
					turfs.get(0).setOwner(demoOwner);
					turfRepository.save(turfs.get(0));
				}
				System.out.println("✅ Standard Demo Turf Owner created: owner@buffturf.com / owner123");
			}
		};
	}
}
