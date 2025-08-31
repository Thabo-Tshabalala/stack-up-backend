package za.ac.cput.stackUpApi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for deserializing the JSON response from the external API.
 * Extracts both id and paymentIdentifier from the nested "user" object.
 */
public class ExternalUserResponse {
    private String id;
    private String paymentIdentifier;

    @JsonProperty("user")
    private void unpackNestedUser(ExternalUserResponse user) {
        this.id = user.id;
        this.paymentIdentifier = user.paymentIdentifier;
    }

    public String getId() {
        return id;
    }

    public String getPaymentIdentifier() {
        return paymentIdentifier;
    }
}
