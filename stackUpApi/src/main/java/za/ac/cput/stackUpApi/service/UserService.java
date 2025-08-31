package za.ac.cput.stackUpApi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import za.ac.cput.stackUpApi.model.User;
import za.ac.cput.stackUpApi.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ExternalApiService externalApiService;

    public UserService(UserRepository userRepository, ExternalApiService externalApiService) {
        this.userRepository = userRepository;
        this.externalApiService = externalApiService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User signup(String email, String rawPassword, String firstName, String lastName, String phoneNumber) {
        var externalUser = externalApiService.createUserExternally(email, firstName, lastName);

        if (externalUser == null || externalUser.getId() == null) {
            throw new RuntimeException("Failed to create user on external API");
        }

        String apiUserId = externalUser.getId();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        String paymentId = externalUser.getPaymentIdentifier();

        User user = new User.Builder()
                .setApiUserId(apiUserId)
                .setPaymentIdentifier(paymentId)
                .setEmail(email)
                .setPasswordHash(hashedPassword)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPhoneNumber(phoneNumber)
                .build();

        logger.info("Saving user locally: {}", user.getEmail());
        User savedUser = userRepository.save(user);
        logger.info("User saved with localId: {}", savedUser.getLocalId());

        return savedUser;
    }


    public Optional<User> login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
