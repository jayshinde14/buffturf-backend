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
								  PasswordEncoder passwordEncoder,
								  org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// 1. Find the foreign key constraint name for slot_id in the bookings table
				String fkQuery = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
								 "WHERE TABLE_NAME = 'bookings' AND COLUMN_NAME = 'slot_id' AND CONSTRAINT_NAME != 'PRIMARY'";
				java.util.List<String> fkNames = jdbcTemplate.queryForList(fkQuery, String.class);
				
				// 2. Drop the foreign key constraint if it exists
				for (String fkName : fkNames) {
					jdbcTemplate.execute("ALTER TABLE bookings DROP FOREIGN KEY " + fkName);
					System.out.println("✅ Dropped foreign key constraint: " + fkName);
				}

				// 3. Drop the column
				jdbcTemplate.execute("ALTER TABLE bookings DROP COLUMN slot_id");
				System.out.println("✅ Successfully dropped old slot_id column from bookings table!");
			} catch (Exception e) {
				System.out.println("ℹ️ Database schema is already clean (no old slot_id column found or already dropped). Details: " + e.getMessage());
			}
			
			if (!userRepository.existsByUsername("admin")) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@buffturf.com");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole(User.Role.ADMIN);
				admin.setPhoneNumber("9999999999");
				userRepository.save(admin);
				System.out.println("✅ Admin created successfully!");
			} else {
				System.out.println("✅ Admin already exists!");
			}
		};
	}
}
