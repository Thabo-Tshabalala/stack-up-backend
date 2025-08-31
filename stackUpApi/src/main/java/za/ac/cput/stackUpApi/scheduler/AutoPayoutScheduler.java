package za.ac.cput.stackUpApi.scheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.service.PoolService;

import java.util.List;

@Component
public class AutoPayoutScheduler {

    private final PoolService poolService;

    public AutoPayoutScheduler(PoolService poolService) {
        this.poolService = poolService;
    }

    //Adjust here to get check if auto pay
    @Scheduled(fixedRate = 180000) // 3 minutes
    @Transactional
    public void run() {
        System.out.println("=== Running scheduled payout ===");

        // Fetch only pools where goal is reached and payout not yet completed
        List<Pool> eligiblePools = poolService.getPoolsWhereGoalReached();
        if (eligiblePools.isEmpty()) {
            System.out.println("No pools with goal reached at this time.");
            return;
        }

        for (Pool pool : eligiblePools) {
            poolService.processScheduledPayouts(pool);
        }
    }
}
