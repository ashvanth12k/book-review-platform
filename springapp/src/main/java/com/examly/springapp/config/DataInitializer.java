package com.examly.springapp.config;

import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a demo publisher account on startup if it does not already exist.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String PUBLISHER_EMAIL = "sunpublisher@gmail.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(PUBLISHER_EMAIL).isPresent()) {
            return;
        }
        User publisher = new User();
        publisher.setName("sunpublisher");
        publisher.setEmail(PUBLISHER_EMAIL);
        publisher.setPassword(passwordEncoder.encode("sun123"));
        publisher.setRole("ROLE_PUBLISHER");
        userRepository.save(publisher);
    }
}
