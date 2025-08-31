package za.ac.cput.stackUpApi.model;

import jakarta.persistence.*;

@Entity
public class PoolInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Pool pool;

    @ManyToOne
    private User invitee;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    public enum Status {
        PENDING, ACCEPTED, DECLINED
    }

    public PoolInvite() {
    }

    private PoolInvite(Builder builder) {
        this.pool = builder.pool;
        this.status = builder.status;
    }

    public Long getId() {
        return id;
    }

    public Pool getPool() {
        return pool;
    }

    public User getInvitee() {
        return invitee;
    }

    public Status getStatus() {
        return status;
    }

    public String getPoolName() {
        return pool != null && pool.getPoolName() != null ? pool.getPoolName() : "Unnamed Pool";
    }


    public void setPool(Pool pool) {
        this.pool = pool;
    }

    public void setInvitee(User invitee) {
        this.invitee = invitee;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static class Builder {
        private Pool pool;
        private Status status = Status.PENDING;

        public Builder pool(Pool pool) {
            this.pool = pool;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public PoolInvite build() {
            return new PoolInvite(this);
        }
    }

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    public void decline() {
        this.status = Status.DECLINED;
    }

}
