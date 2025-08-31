package za.ac.cput.stackUpApi.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${WHATSAPP_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${WHATSAPP_PHONE_NUMBER_ID}")
    private String phoneNumberId;

    @Value("${WHATSAPP_API_URL:https://graph.facebook.com/v22.0}")
    private String apiUrl;

    private String url;

    @PostConstruct
    private void init() {
        url = apiUrl + "/" + phoneNumberId + "/messages";
        System.out.println("WhatsApp Service initialized. URL: " + url);
    }

    /**
     * Send a WhatsApp template message
     * @param to recipient number in E.164 format (e.g., "27719453921")
     * @param templateName approved template name (e.g., "hello_world")
     * @param languageCode template language code (e.g., "en_US")
     */
    public void sendTemplateMessage(String to, String templateName, String languageCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        String payload = String.format(
                "{ \"messaging_product\": \"whatsapp\", " +
                        "\"to\": \"%s\", " +
                        "\"type\": \"template\", " +
                        "\"template\": { \"name\": \"%s\", \"language\": { \"code\": \"%s\" } } }",
                to, templateName, languageCode
        );

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("✅ WhatsApp template sent to " + to + ": " + response.getBody());
        } catch (HttpStatusCodeException e) {
            System.err.println("❌ Failed to send WhatsApp template to " + to);
            System.err.println("Status code: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error sending WhatsApp template to " + to + ": " + e.getMessage());
        }
    }
}