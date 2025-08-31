package za.ac.cput.stackUpApi.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long localId;

    private String apiUserId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column(nullable = true, unique = true)
    private String paymentIdentifier; // store the external payment ID

    @Column(nullable = true, unique = true)
    private String phoneNumber;


    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private java.util.List<Pool> pools;

    protected User() {
    }

    private User(Builder builder) {
        this.apiUserId = builder.apiUserId;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.paymentIdentifier = builder.paymentIdentifier;
        this.phoneNumber = builder.phoneNumber; // Set phone number
    }

    public Long getLocalId() {
        return localId;
    }

    public String getApiUserId() {
        return apiUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPaymentIdentifier() {
        return paymentIdentifier;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public static class Builder {
        private String apiUserId;
        private String email;
        private String passwordHash;
        private String firstName;
        private String lastName;
        private String paymentIdentifier;
        private String phoneNumber;

        public Builder setPaymentIdentifier(String paymentIdentifier) {
            this.paymentIdentifier = paymentIdentifier;
            return this;
        }

        public Builder setApiUserId(String apiUserId) {
            this.apiUserId = apiUserId;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
