package za.ac.cput.stackUpApi.helper;

import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.model.PayoutSchedule;
import za.ac.cput.stackUpApi.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PoolHelper {

    /**
     * Generate payout schedule for pool based on rotation method (FIXED or RANDOM). FOR NOW RANDOM
     */
    public static List<PayoutSchedule> generatePayoutSchedule(Pool pool) {
        List<User> members = new ArrayList<>(pool.getMembers());
        if (members.isEmpty()) return new ArrayList<>();

        if ("RANDOM".equalsIgnoreCase(pool.getRotationMethod())) {
            Collections.shuffle(members);
        }
        // FIXED preserves order

        List<PayoutSchedule> scheduleList = new ArrayList<>();
        LocalDate currentDate = pool.getStartDate() != null ? pool.getStartDate() : LocalDate.now();
        int position = 1;

        for (User member : members) {
            PayoutSchedule schedule = new PayoutSchedule.Builder()
                    .setPool(pool)
                    .setMember(member)
                    .setPosition(position++)
                    .setPayoutDate(currentDate.atStartOfDay())
                    .setStatus(PayoutSchedule.STATUS_PENDING)
                    .build();

            scheduleList.add(schedule);

            // Increment date based on pool frequency
            switch (pool.getFrequency().toLowerCase()) {
                case "weekly" -> currentDate = currentDate.plusWeeks(1);
                case "monthly" -> currentDate = currentDate.plusMonths(1);
                default -> currentDate = currentDate.plusDays(1);
            }
        }

        return scheduleList;
    }

    /**
     * Checks if all payouts for a pool are completed.
     */

    public static boolean isPoolCompleted(Pool pool) {
        return pool.getPayoutSchedule().stream()
                .allMatch(s -> PayoutSchedule.STATUS_PAID.equals(s.getStatus()));
    }

    /**
     * Lock the payout order if first payout is done (used for FIXED pools).
     */

    public static void lockFixedPoolIfFirstPaid(Pool pool) {
        if (!pool.isPayoutOrderLocked() &&
                pool.getPayoutSchedule().stream().anyMatch(s -> PayoutSchedule.STATUS_PAID.equals(s.getStatus()))) {
            pool.setPayoutOrderLocked(true);
        }
    }

    /**
     * Check if pool has reached its goal based on external balance
     */
    public static boolean isGoalReached(Pool pool, double collectedAmount) {
        return collectedAmount >= pool.getGoal();
    }
}
