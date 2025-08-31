package za.ac.cput.stackUpApi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${WHATSAPP_ACCESS_TOKEN}")
    private String ACCESS_TOKEN;

    @Value("${WHATSAPP_PHONE_NUMBER_ID}")
    private String PHONE_NUMBER_ID;

    private String API_URL;

    @Value("${WHATSAPP_API_URL:https://graph.facebook.com/v22.0}")
    private String API_BASE_URL;

    @PostConstruct
    private void init() {
        API_URL = API_BASE_URL + "/" + PHONE_NUMBER_ID + "/messages";
        logger.info("WhatsApp Service initialized. URL: {}", API_URL);
        logger.info("Access token loaded: {}", ACCESS_TOKEN != null);
        logger.info("Phone number ID loaded: {}", PHONE_NUMBER_ID);
    }

    public void sendTextMessage(String to, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ACCESS_TOKEN);

        String payload = String.format(
                "{\"messaging_product\": \"whatsapp\", \"to\": \"%s\", \"text\": {\"body\": \"%s\"}}",
                to, message
        );

        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
            logger.info("✅ WhatsApp text sent to {}: {}", to, response.getBody());
        } catch (HttpStatusCodeException e) {
            logger.error("❌ Failed to send WhatsApp message to {}", to);
            logger.error("Status code: {}", e.getStatusCode());
            logger.error("Response body: {}", e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending WhatsApp message to {}: {}", to, e.getMessage());
        }
    }
}