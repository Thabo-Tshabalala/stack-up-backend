package za.ac.cput.stackUpApi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.stackUpApi.model.PayoutSchedule;
import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.model.User;
import java.util.List;

@Repository
public interface PayoutScheduleRepository extends JpaRepository<PayoutSchedule, Long> {

    List<PayoutSchedule> findByPoolAndStatusOrderByPositionAsc(Pool pool, String status);

    boolean existsByPoolAndMemberAndStatus(Pool pool, User member, String status);

}
