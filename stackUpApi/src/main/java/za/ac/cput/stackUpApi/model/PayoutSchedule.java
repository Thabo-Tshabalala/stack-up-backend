package za.ac.cput.stackUpApi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_schedules")
public class PayoutSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    private int position;
    private LocalDateTime payoutDate;

    @Column(length = 20)
    private String status;
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";

    protected PayoutSchedule() {}

    private PayoutSchedule(Builder builder) {
        this.pool = builder.pool;
        this.member = builder.member;
        this.position = builder.position;
        this.payoutDate = builder.payoutDate;
        this.status = builder.status;
    }

    public Pool getPool() { return pool; }
    public User getMember() { return member; }
    public LocalDateTime getPayoutDate() { return payoutDate; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public static class Builder {
        private Pool pool;
        private User member;
        private int position;
        private LocalDateTime payoutDate;
        private String status;

        public Builder setPool(Pool pool) { this.pool = pool; return this; }
        public Builder setMember(User member) { this.member = member; return this; }
        public Builder setPosition(int position) { this.position = position; return this; }
        public Builder setPayoutDate(LocalDateTime payoutDate) { this.payoutDate = payoutDate; return this; }
        public Builder setStatus(String status) { this.status = status; return this; }
        public PayoutSchedule build() { return new PayoutSchedule(this); }
    }
}
