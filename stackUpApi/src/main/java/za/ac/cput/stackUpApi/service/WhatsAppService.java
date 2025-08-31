package za.ac.cput.stackUpApi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class WhatsAppService {

    private final String accessToken;
    private final String phoneNumberId;
    private final String apiUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppService(
            @Value("${WHATSAPP_ACCESS_TOKEN}") String accessToken,
            @Value("${WHATSAPP_PHONE_NUMBER_ID}") String phoneNumberId
    ) {
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.apiUrl = "https://graph.facebook.com/v22.0/" + phoneNumberId + "/messages";

        System.out.println("WhatsApp Access Token Loaded: " + (accessToken != null));
        System.out.println("Phone Number ID Loaded: " + phoneNumberId);
    }


    public void sendTextMessage(String to, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        String payload = String.format(
                "{\"messaging_product\": \"whatsapp\", \"to\": \"%s\", \"text\": {\"body\": \"%s\"}}",
                to, message
        );

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            System.out.println("WhatsApp text sent to " + to + ": " + response.getBody());
        } catch (HttpStatusCodeException e) {

            System.err.println("Failed to send WhatsApp message to " + to);
            System.err.println("Status code: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Unexpected error sending WhatsApp message to " + to + ": " + e.getMessage());
        }
    }
}
