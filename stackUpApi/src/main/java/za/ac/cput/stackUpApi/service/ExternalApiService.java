package za.ac.cput.stackUpApi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import za.ac.cput.stackUpApi.dto.ExternalUserResponse;
import za.ac.cput.stackUpApi.model.User;

import java.util.List;
import java.util.Map;

@Service
public class ExternalApiService {

    private final WebClient webClient;
    private final String token;

    public ExternalApiService(
            @Value("${EXTERNAL_API_BASE_URL}") String baseUrl,
            @Value("${EXTERNAL_API_TOKEN}") String token
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    public ExternalUserResponse createUserExternally(String email, String firstName, String lastName) {
        User requestBody = new User.Builder()
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .build();

        ExternalUserResponse response = webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ExternalUserResponse.class)
                .block();

        if (response == null) throw new RuntimeException("Failed to parse external API response");
        return response;
    }

    public ExternalUserResponse getUserById(String id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ExternalUserResponse.class)
                .block();
    }

    public Map<String, Object> getWalletFloat() {
        return webClient.get()
                .uri("/float")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Double getUserBalance(String userId) {
        try {
            Map response = webClient.get()
                    .uri("/{userId}/balance", userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("tokens")) {
                List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.get("tokens");
                return tokens.stream()
                        .filter(t -> "L ZAR Coin".equals(t.get("name")))
                        .map(t -> Double.parseDouble(t.get("balance").toString()))
                        .findFirst()
                        .orElse(0.0);
            }
            return 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public ExternalUserResponse updateUserExternally(String id, String email, String firstName, String lastName, String imageUrl) {
        Map<String, Object> requestBody = Map.of(
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "imageUrl", imageUrl
        );

        ExternalUserResponse response = webClient.put()
                .uri("/users/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ExternalUserResponse.class)
                .block();

        if (response == null) throw new RuntimeException("Failed to parse external API response");
        return response;
    }

    public boolean activateGasPayment(String apiUserId) {
        try {
            String response = webClient.post()
                    .uri("/activate-pay/{userId}", apiUserId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response != null && !response.isBlank();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean transferStablecoin(String apiUserId, String transactionRecipient, double amount, String notes) {
        if (!activateGasPayment(apiUserId)) {
            throw new RuntimeException("Failed to activate gas for user: " + apiUserId);
        }

        Map<String, Object> requestBody = Map.of(
                "transactionAmount", amount,
                "transactionRecipient", transactionRecipient,
                "transactionNotes", notes != null ? notes : ""
        );

        try {
            String response = webClient.post()
                    .uri("/transfer/{userId}", apiUserId)
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response != null && !response.isBlank();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("External API transfer failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during transfer", e);
        }
    }
}
