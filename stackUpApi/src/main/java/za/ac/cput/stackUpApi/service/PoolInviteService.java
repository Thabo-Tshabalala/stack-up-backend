package za.ac.cput.stackUpApi.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.stackUpApi.model.PoolInvite;
import za.ac.cput.stackUpApi.model.User;
import za.ac.cput.stackUpApi.repository.PoolInviteRepository;
import za.ac.cput.stackUpApi.repository.PoolRepository;
import za.ac.cput.stackUpApi.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;

@Service
public class PoolInviteService {

    private final PoolInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final PoolRepository poolRepository;

    public PoolInviteService(PoolInviteRepository inviteRepository,
                             UserRepository userRepository,
                             PoolRepository poolRepository) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.poolRepository = poolRepository;
    }

    public List<PoolInvite> getPendingInvites(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inviteRepository.findByInviteeAndStatus(user, PoolInvite.Status.PENDING);
    }

    @Transactional
    public void respondToInvite(Long inviteId, PoolInvite.Status status) {
        PoolInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (status == PoolInvite.Status.ACCEPTED) {
            invite.accept();
            if (invite.getPool().getMembers() == null) {
                invite.getPool().setMembers(new ArrayList<>());
            }
            if (!invite.getPool().getMembers().contains(invite.getInvitee())) {
                invite.getPool().getMembers().add(invite.getInvitee());
            }
            poolRepository.save(invite.getPool());
            inviteRepository.save(invite);
        } else if (status == PoolInvite.Status.DECLINED) {
            invite.decline();
            inviteRepository.save(invite);
            inviteRepository.delete(invite); // here I might just flag the invite as declined for future references
        }
    }

}
