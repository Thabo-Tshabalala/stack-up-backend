package za.ac.cput.stackUpApi.controller;

import org.springframework.web.bind.annotation.*;
import za.ac.cput.stackUpApi.model.PoolInvite;
import za.ac.cput.stackUpApi.service.PoolInviteService;
import za.ac.cput.stackUpApi.service.JwtService;
import za.ac.cput.stackUpApi.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pools-invite")
public class PoolInviteController {

    private final PoolInviteService inviteService;
    private final JwtService jwtService;

    public PoolInviteController(PoolInviteService inviteService,
                                JwtService jwtService,
                                UserService userService) {
        this.inviteService = inviteService;
        this.jwtService = jwtService;
    }

    @GetMapping("/my-invites")
    public List<PoolInvite> getMyInvites(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String email = jwtService.getEmailFromToken(token);
        return inviteService.getPendingInvites(email);
    }

    @PatchMapping("/invite/{inviteId}")
    public void respondToInvite(@PathVariable Long inviteId, @RequestBody Map<String, String> body) {
        PoolInvite.Status status = PoolInvite.Status.valueOf(body.get("status").toUpperCase());
        inviteService.respondToInvite(inviteId, status);
    }
}
