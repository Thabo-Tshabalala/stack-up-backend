package za.ac.cput.stackUpApi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import za.ac.cput.stackUpApi.helper.PoolHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pools")
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String poolName;
    private Double goal;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;

    @JsonProperty
    private String paymentIdentifier;

    @JsonProperty
    private String externalUserId;

    private String category;
    private String rotationMethod; // FIXED or RANDOM
    private boolean isPayoutOrderLocked = false;
    private boolean isPoolCompleted = false;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "pool_members",
            joinColumns = @JoinColumn(name = "pool_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    @OneToMany(mappedBy = "pool", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PayoutSchedule> payoutSchedule = new ArrayList<>();

    protected Pool() {
    }

    private Pool(Builder builder) {
        this.poolName = builder.poolName;
        this.goal = builder.goal;
        this.frequency = builder.frequency;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.creator = builder.creator;
        this.members = builder.members != null ? new ArrayList<>(builder.members) : new ArrayList<>();
        this.payoutSchedule = builder.payoutSchedule != null ? new ArrayList<>(builder.payoutSchedule) : new ArrayList<>();
        this.paymentIdentifier = builder.paymentIdentifier;
        this.externalUserId = builder.externalUserId;
        this.category = builder.category;
        this.rotationMethod = builder.rotationMethod;
        this.isPayoutOrderLocked = builder.isPayoutOrderLocked;
        this.isPoolCompleted = builder.isPoolCompleted;
    }

    public String getPoolName() {
        return poolName;
    }

    public Double getGoal() {
        return goal;
    }

    public String getFrequency() {
        return frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public User getCreator() {
        return creator;
    }

    public List<User> getMembers() {
        return members;
    }

    public String getPaymentIdentifier() {
        return paymentIdentifier;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getCategory() {
        return category;
    }

    public String getRotationMethod() {
        return rotationMethod;
    }

    public boolean isPayoutOrderLocked() {
        return isPayoutOrderLocked;
    }

    public boolean isPoolCompleted() {
        return isPoolCompleted;
    }

    public List<PayoutSchedule> getPayoutSchedule() {
        return payoutSchedule;
    }


    public void setMembers(List<User> members) {
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public void addMember(User user) {
        if (members == null) members = new ArrayList<>();
        members.add(user);
    }

    public void setPayoutSchedule(List<PayoutSchedule> payoutSchedule) {
        this.payoutSchedule = payoutSchedule != null ? new ArrayList<>(payoutSchedule) : new ArrayList<>();
    }

    public void setPayoutOrderLocked(boolean locked) {
        this.isPayoutOrderLocked = locked;
    }

    public void setPoolCompleted(boolean completed) {
        this.isPoolCompleted = completed;
    }


    public static class Builder {
        private String poolName;
        private Double goal;
        private String frequency;
        private LocalDate startDate;
        private LocalDate endDate;
        private User creator;
        private List<User> members;
        private String paymentIdentifier;
        private String externalUserId;
        private String category;
        private String rotationMethod;
        private boolean isPayoutOrderLocked = false;
        private boolean isPoolCompleted = false;
        private List<PayoutSchedule> payoutSchedule;

        public Builder setPoolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public Builder setGoal(Double goal) {
            this.goal = goal;
            return this;
        }

        public Builder setFrequency(String frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder setEndDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder setCreator(User creator) {
            this.creator = creator;
            return this;
        }

        public Builder setMembers(List<User> members) {
            this.members = members != null ? new ArrayList<>(members) : null;
            return this;
        }

        public Builder setPaymentIdentifier(String paymentIdentifier) {
            this.paymentIdentifier = paymentIdentifier;
            return this;
        }

        public Builder setExternalUserId(String externalUserId) {
            this.externalUserId = externalUserId;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public Builder setRotationMethod(String rotationMethod) {
            this.rotationMethod = rotationMethod;
            return this;
        }

        public Pool build() {
            return new Pool(this);
        }
    }
}
