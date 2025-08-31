package za.ac.cput.stackUpApi.repository;

import org.springframework.stereotype.Repository;
import za.ac.cput.stackUpApi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByApiUserId(String apiUserId);
    Optional<User> findByPhoneNumber(String phoneNumber);
}
