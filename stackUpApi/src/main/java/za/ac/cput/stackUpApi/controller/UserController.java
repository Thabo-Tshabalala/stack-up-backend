package za.ac.cput.stackUpApi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.stackUpApi.model.User;
import za.ac.cput.stackUpApi.service.ExternalApiService;
import za.ac.cput.stackUpApi.service.JwtService;
import za.ac.cput.stackUpApi.service.UserService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final ExternalApiService externalApiService;

    @Autowired
    public UserController(UserService userService, JwtService jwtService, ExternalApiService externalApiService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.externalApiService = externalApiService;
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String firstName = payload.get("firstName");
        String lastName = payload.get("lastName");
        String phoneNumber = payload.get("phoneNumber"); // <-- new

        if (email == null || password == null || firstName == null || lastName == null || phoneNumber == null) {
            return ResponseEntity.badRequest().body("email, password, firstName, lastName, and phoneNumber are required");
        }

        try {
            User user = userService.signup(email, password, firstName, lastName, phoneNumber); // <-- pass it
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Signup failed: " + e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("email and password are required");
        }

        Optional<User> userOpt = userService.login(email, password);

        if (userOpt.isPresent()) {
            String token = jwtService.generateToken(email);
            return ResponseEntity.ok(Map.of("token", token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        String email = jwtService.getEmailFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(userOpt.get());
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet() {
        try {
            Map<String, Object> walletData = externalApiService.getWalletFloat();
            return ResponseEntity.ok(walletData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch wallet: " + e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUserExternally(
            @PathVariable String id,
            @RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        String firstName = payload.get("firstName");
        String lastName = payload.get("lastName");
        String imageUrl = payload.get("imageUrl");

        try {
            var updatedUser = externalApiService.updateUserExternally(id, email, firstName, lastName, imageUrl);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update user: " + e.getMessage());
        }
    }
}
