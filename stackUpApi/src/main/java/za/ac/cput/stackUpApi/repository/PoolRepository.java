package za.ac.cput.stackUpApi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoolRepository extends JpaRepository<Pool, Long> {
    List<Pool> findByMembersContaining(User user);
    Optional<Pool> findByPaymentIdentifier(String paymentIdentifier);
    Optional<Pool> findByExternalUserId(String externalId);

}
