package com.buffturf.buffturf_backend;

import com.buffturf.buffturf_backend.model.User;
import com.buffturf.buffturf_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;

@SpringBootApplication
public class BuffturfBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuffturfBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner createAdmin(UserRepository userRepository,
								  PasswordEncoder passwordEncoder) {
		return args -> {
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
