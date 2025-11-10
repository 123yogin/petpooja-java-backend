package com.example.petpooja_clone;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetpoojaCloneApplication {

	public static void main(String[] args) {
		// Load .env file if it exists
		// Try multiple locations: project root, current directory, and user directory
		try {
			Dotenv dotenv = null;
			
			// Try project root (where .env should be)
			try {
				dotenv = Dotenv.configure()
						.directory(".")
						.ignoreIfMissing()
						.load();
			} catch (Exception e1) {
				// Try user directory
				try {
					dotenv = Dotenv.configure()
							.directory(System.getProperty("user.dir"))
							.ignoreIfMissing()
							.load();
				} catch (Exception e2) {
					// Try parent directory
					try {
						java.io.File currentDir = new java.io.File(".");
						dotenv = Dotenv.configure()
								.directory(currentDir.getAbsolutePath())
								.ignoreIfMissing()
								.load();
					} catch (Exception e3) {
						// Last try: look for .env in classpath or common locations
						dotenv = Dotenv.configure()
								.ignoreIfMissing()
								.load();
					}
				}
			}
			
			if (dotenv != null) {
				// Set system properties from .env file
				dotenv.entries().forEach(entry -> {
					System.setProperty(entry.getKey(), entry.getValue());
				});
				System.out.println("✓ Loaded .env file successfully");
			} else {
				System.out.println("⚠ No .env file found. Using environment variables and defaults.");
			}
		} catch (Exception e) {
			System.out.println("⚠ Error loading .env file: " + e.getMessage());
			System.out.println("⚠ Using environment variables and defaults from application.properties");
		}
		
		SpringApplication.run(PetpoojaCloneApplication.class, args);
	}

}
