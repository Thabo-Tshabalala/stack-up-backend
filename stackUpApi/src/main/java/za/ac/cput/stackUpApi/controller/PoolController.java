package za.ac.cput.stackUpApi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.stackUpApi.model.Pool;
import za.ac.cput.stackUpApi.model.User;
import za.ac.cput.stackUpApi.service.PoolService;
import za.ac.cput.stackUpApi.service.UserService;
import za.ac.cput.stackUpApi.service.JwtService;
import za.ac.cput.stackUpApi.service.WhatsAppService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pools")
public class PoolController {

    private final PoolService poolService;
    private final UserService userService;
    private final JwtService jwtService;
    private final WhatsAppService whatsAppService;

    public PoolController(PoolService poolService, UserService userService, JwtService jwtService, WhatsAppService whatsAppService) {
        this.poolService = poolService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pool> getPoolById(@PathVariable Long id) {
        return poolService.getPoolById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/my-active")
    public ResponseEntity<List<Map<String, Object>>> getMyActivePools(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtService.getEmailFromToken(token);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Map<String, Object>> activePools = poolService.getActivePoolsForUserWithLiveAmount(user);
        return ResponseEntity.ok(activePools);
    }


    @PostMapping
    public ResponseEntity<Pool> createPool(@RequestBody Pool pool) {
        Pool createdPool = poolService.createPool(pool);
        return ResponseEntity.ok(createdPool);
    }


    @PostMapping("/contribute")
    public ResponseEntity<Pool> contributeToPool(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String poolPaymentId,
            @RequestParam double amount,
            @RequestParam(required = false) String notes
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtService.getEmailFromToken(token);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Pool updatedPool = poolService.contributeToPool(user.getApiUserId(), poolPaymentId, amount, notes);


            double currentTotal = poolService.getCollectedAmount(updatedPool);


            updatedPool.getMembers().forEach(member -> {
                if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {


                    String to = member.getPhoneNumber().replaceFirst("^0", "27");

                    String message = String.format(
                            "%s contributed R%.2f to pool '%s'. Current pool total: R%.2f",
                            user.getFirstName(),
                            amount,
                            updatedPool.getPoolName(),
                            currentTotal
                    );

                    try {
                        whatsAppService.sendTextMessage(to, message);
                    } catch (Exception e) {
                        System.err.println("Failed to notify " + to + ": " + e.getMessage());
                    }
                }
            });


            return ResponseEntity.ok(updatedPool);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(null);
        }
    }


    @PostMapping("/{poolUuid}/invite")
    public ResponseEntity<?> inviteMember(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String poolUuid,
            @RequestBody Map<String, String> body
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String inviterEmail = jwtService.getEmailFromToken(token);
        User inviter = userService.findByEmail(inviterEmail)
                .orElseThrow(() -> new RuntimeException("Inviter not found"));

        String inviteeEmail = body.get("email");
        if (inviteeEmail == null || inviteeEmail.isEmpty()) {
            return ResponseEntity.badRequest().body("Invitee email is required");
        }

        try {
            var invite = poolService.inviteMemberToPoolByUuid(poolUuid, inviter, inviteeEmail);


            User invitee = userService.findByEmail(inviteeEmail)
                    .orElse(null);
            if (invitee != null && invitee.getPhoneNumber() != null && !invitee.getPhoneNumber().isEmpty()) {
                String to = invitee.getPhoneNumber().replaceFirst("^0", "27");
                String message = String.format(
                        "%s invited you to join the pool '%s'!",
                        inviter.getFirstName(),
                        invite.getPoolName()
                );
                try {
                    whatsAppService.sendTextMessage(to, message);
                } catch (Exception e) {
                    System.err.println("Failed to notify invitee " + to + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(invite);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendToUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String recipientId,
            @RequestParam double amount
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        String email = jwtService.getEmailFromToken(token);
        User sender = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        try {
            poolService.sendToUser(sender.getApiUserId(), recipientId, amount);


            User recipient = userService.findByEmail(recipientId)
                    .orElse(null);
            if (recipient != null && recipient.getPhoneNumber() != null && !recipient.getPhoneNumber().isEmpty()) {
                String to = recipient.getPhoneNumber().replaceFirst("^0", "27");
                String message = String.format(
                        "%s sent you R%.2f",
                        sender.getFirstName(),
                        amount
                );
                try {
                    whatsAppService.sendTextMessage(to, message);
                } catch (Exception e) {
                    System.err.println("Failed to notify " + to + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok("Sent R" + amount + " to user " + recipientId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

}
