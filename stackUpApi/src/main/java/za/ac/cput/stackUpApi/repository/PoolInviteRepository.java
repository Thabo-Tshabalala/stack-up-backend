package za.ac.cput.stackUpApi.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.model.PoolInvite;
import za.ac.cput.stackUpApi.model.User;
import java.util.List;

public interface PoolInviteRepository extends JpaRepository<PoolInvite, Long> {
    List<PoolInvite> findByInviteeAndStatus(User invitee, PoolInvite.Status status);
    boolean existsByPoolAndInvitee(Pool pool, User invitee);
}
