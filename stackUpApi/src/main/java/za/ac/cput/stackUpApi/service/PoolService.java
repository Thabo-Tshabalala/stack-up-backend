package za.ac.cput.stackUpApi.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.stackUpApi.helper.PoolHelper;
import za.ac.cput.stackUpApi.model.*;
import za.ac.cput.stackUpApi.repository.*;
import za.ac.cput.stackUpApi.dto.ExternalUserResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PoolService {

    private final PoolRepository poolRepository;
    private final UserRepository userRepository;
    private final PoolInviteRepository poolInviteRepository;
    private final ExternalApiService externalApiService;
    private final PayoutScheduleRepository payoutScheduleRepository;
    private final WhatsAppService whatsAppService;


    public PoolService(PoolRepository poolRepository,
                       UserRepository userRepository,
                       PoolInviteRepository poolInviteRepository,
                       ExternalApiService externalApiService,
                       PayoutScheduleRepository payoutScheduleRepository, WhatsAppService whatsAppService) {
        this.poolRepository = poolRepository;
        this.userRepository = userRepository;
        this.poolInviteRepository = poolInviteRepository;
        this.externalApiService = externalApiService;
        this.payoutScheduleRepository = payoutScheduleRepository;
        this.whatsAppService = whatsAppService;
    }

    @Transactional
    public Pool createPool(Pool pool) {
        User creator = userRepository.findByApiUserId(pool.getCreator().getApiUserId())
                .orElseThrow(() -> new RuntimeException("Creator not found in DB"));

        List<User> members = pool.getMembers() != null ? pool.getMembers() : new ArrayList<>();
        if (members.stream().noneMatch(m -> m.getEmail().equals(creator.getEmail()))) {
            members.add(creator);
        }

        List<User> persistentMembers = members.stream()
                .map(member -> userRepository.findByEmail(member.getEmail())
                        .orElseThrow(() -> new RuntimeException("Member not found: " + member.getEmail())))
                .collect(Collectors.toList());

        String poolEmail = "pool_" + UUID.randomUUID() + "@stackup.app";
        ExternalUserResponse externalPoolUser = externalApiService.createUserExternally(
                poolEmail,
                "Pool",
                pool.getPoolName()
        );

        if (externalPoolUser == null || externalPoolUser.getPaymentIdentifier() == null || externalPoolUser.getId() == null) {
            throw new RuntimeException("Failed to create pool payment account externally");
        }

        Pool poolToSave = new Pool.Builder()
                .setPoolName(pool.getPoolName())
                .setGoal(pool.getGoal())
                .setFrequency(pool.getFrequency())
                .setStartDate(pool.getStartDate())
                .setEndDate(pool.getEndDate())
                .setCreator(creator)
                .setMembers(List.of(creator))
                .setPaymentIdentifier(externalPoolUser.getPaymentIdentifier())
                .setExternalUserId(externalPoolUser.getId())
                .setCategory(pool.getCategory())
                .setRotationMethod(pool.getRotationMethod())
                .build();

        poolToSave = poolRepository.save(poolToSave);

        List<PayoutSchedule> scheduleList = PoolHelper.generatePayoutSchedule(poolToSave);
        poolToSave.setPayoutSchedule(scheduleList);
        payoutScheduleRepository.saveAll(scheduleList);
        poolRepository.save(poolToSave); // ensure relation is saved

        for (User member : persistentMembers) {
            if (!member.getEmail().equals(creator.getEmail())) {
                PoolInvite invite = new PoolInvite();
                invite.setPool(poolToSave);
                invite.setInvitee(member);
                invite.setStatus(PoolInvite.Status.PENDING);
                poolInviteRepository.save(invite);


                if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                    String to = member.getPhoneNumber().replaceFirst("^0", "27");
                    String message = String.format(
                            "%s invited you to join the pool '%s'.",
                            creator.getFirstName(),
                            poolToSave.getPoolName()
                    );
                    try {
                        whatsAppService.sendTextMessage(to, message);
                    } catch (Exception e) {
                        System.err.println("Failed to notify " + to + ": " + e.getMessage());
                    }
                }
            }
        }

        return poolToSave;
    }


    @Transactional
    public Pool contributeToPool(String apiUserId, String poolPaymentId, double amount, String notes) {
        if (apiUserId == null || poolPaymentId == null || amount <= 0)
            throw new IllegalArgumentException("Invalid contribution parameters");

        User user = userRepository.findByApiUserId(apiUserId)
                .orElseThrow(() -> new RuntimeException("User not found for API ID: " + apiUserId));

        Pool pool = poolRepository.findByPaymentIdentifier(poolPaymentId)
                .orElseThrow(() -> new RuntimeException("Pool not found for paymentIdentifier: " + poolPaymentId));

        boolean success = externalApiService.transferStablecoin(
                user.getApiUserId(),
                pool.getPaymentIdentifier(),
                amount,
                notes
        );

        if (!success) throw new RuntimeException("Failed to transfer stablecoins");

        if (!pool.getMembers().contains(user)) pool.addMember(user);
        return poolRepository.save(pool);
    }


    @Transactional
    public PoolInvite inviteMemberToPoolByUuid(String poolUuid, User inviter, String inviteeEmail) {
        Pool pool = poolRepository.findByExternalUserId(poolUuid)
                .orElseThrow(() -> new RuntimeException("Pool not found with UUID: " + poolUuid));

        User invitee = userRepository.findByEmail(inviteeEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + inviteeEmail));

        if (poolInviteRepository.existsByPoolAndInvitee(pool, invitee))
            throw new RuntimeException("User already invited to this pool");

        PoolInvite invite = new PoolInvite();
        invite.setPool(pool);
        invite.setInvitee(invitee);
        invite.setStatus(PoolInvite.Status.PENDING);

        return poolInviteRepository.save(invite);
    }

    public Double getCollectedAmount(Pool pool) {
        if (pool.getExternalUserId() == null) return 0.0;
        try {
            return externalApiService.getUserBalance(pool.getExternalUserId());
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Optional<Pool> getPoolById(Long id) { return poolRepository.findById(id); }

    public List<Map<String, Object>> getActivePoolsForUserWithLiveAmount(User user) {
        List<Pool> pools = poolRepository.findByMembersContaining(user);
        return pools.stream().map(pool -> {
            double liveAmount = getCollectedAmount(pool);
            return Map.of(
                    "id", pool.getExternalUserId(),
                    "poolName", pool.getPoolName(),
                    "goal", pool.getGoal(),
                    "currentAmount", liveAmount,
                    "frequency", pool.getFrequency(),
                    "startDate", pool.getStartDate(),
                    "endDate", pool.getEndDate(),
                    "paymentIdentifier", pool.getPaymentIdentifier(),
                    "createdBy", pool.getCreator().getEmail(),
                    "members", pool.getMembers()
            );
        }).toList();
    }


    public List<Pool> getPoolsWhereGoalReached() {
        return poolRepository.findAll()
                .stream()
                .filter(pool -> {
                    double collectedAmount = externalApiService.getUserBalance(pool.getExternalUserId());
                    return PoolHelper.isGoalReached(pool, collectedAmount) && !pool.isPoolCompleted();
                })
                .toList();
    }

    @Transactional
    public void processScheduledPayouts(Pool pool) {
        double collectedAmount = externalApiService.getUserBalance(pool.getExternalUserId());
        if (!PoolHelper.isGoalReached(pool, collectedAmount)) {
            System.out.println("Pool goal not reached yet: " + pool.getPoolName());
            return;
        }

        List<PayoutSchedule> pendingSchedules = payoutScheduleRepository
                .findByPoolAndStatusOrderByPositionAsc(pool, PayoutSchedule.STATUS_PENDING)
                .stream()
                .filter(schedule -> !schedule.getPayoutDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        if (pendingSchedules.isEmpty()) {
            System.out.println("No pending payouts for pool: " + pool.getPoolName());
            return;
        }


        Optional<PayoutSchedule> nextPayoutOpt = pendingSchedules.stream()
                .filter(schedule -> !payoutScheduleRepository.existsByPoolAndMemberAndStatus(
                        pool,
                        schedule.getMember(),
                        PayoutSchedule.STATUS_PAID
                ))
                .findFirst();

        if (nextPayoutOpt.isEmpty()) {
            System.out.println("All members already paid in this cycle for pool: " + pool.getPoolName());
            return;
        }

        PayoutSchedule schedule = nextPayoutOpt.get();

        try {
            externalApiService.activateGasPayment(pool.getExternalUserId());

            boolean success = externalApiService.transferStablecoin(
                    pool.getExternalUserId(),
                    schedule.getMember().getPaymentIdentifier(),
                    collectedAmount,
                    "Payout for pool " + pool.getPoolName()
            );

            if (success) {

                schedule.setStatus(PayoutSchedule.STATUS_PAID);
                payoutScheduleRepository.save(schedule);

                System.out.println("Payout successful for member: " + schedule.getMember().getEmail());


                User member = schedule.getMember();
                if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                    String to = member.getPhoneNumber().replaceFirst("^0", "27");
                    String message = String.format(
                            "Hi %s! You have received R%.2f from the pool '%s'. Enjoy!",
                            member.getFirstName(),
                            collectedAmount,
                            pool.getPoolName()
                    );
                    try {
                        whatsAppService.sendTextMessage(to, message);
                    } catch (Exception e) {
                        System.err.println("Failed to notify member " + to + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing payout for member: " + schedule.getMember().getEmail() +
                    " in pool: " + pool.getPoolName() + " | " + e.getMessage());
        }

        if ("FIXED".equalsIgnoreCase(pool.getRotationMethod())) {
            PoolHelper.lockFixedPoolIfFirstPaid(pool);
        }

        pool.setPoolCompleted(PoolHelper.isPoolCompleted(pool));
        poolRepository.save(pool);
    }



    @Transactional
    public void sendToUser(String senderApiUserId, String recipientIdentifier, double amount) {
        if (senderApiUserId == null || recipientIdentifier == null || amount <= 0) {
            throw new IllegalArgumentException("Invalid send parameters");
        }

        User sender = userRepository.findByApiUserId(senderApiUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found for API ID: " + senderApiUserId));

        Optional<User> recipientOpt;
        if (recipientIdentifier.contains("@")) {
            recipientOpt = userRepository.findByEmail(recipientIdentifier);
        } else if (recipientIdentifier.matches("\\+?[0-9]+")) {
            recipientOpt = userRepository.findByPhoneNumber(recipientIdentifier);
        } else {
            recipientOpt = userRepository.findByApiUserId(recipientIdentifier);
        }

        User recipient = recipientOpt
                .orElseThrow(() -> new RuntimeException("Recipient not found for identifier: " + recipientIdentifier));

        if (recipient.getApiUserId() == null) {
            throw new RuntimeException("Recipient does not have a valid API user ID");
        }

        boolean success = externalApiService.transferStablecoin(
                sender.getApiUserId(),
                recipient.getPaymentIdentifier(),
                amount,
                "Send from user " + sender.getEmail()
        );

        if (!success) {
            throw new RuntimeException("Failed to send stablecoins to recipient: " + recipientIdentifier);
        }
    }
}
