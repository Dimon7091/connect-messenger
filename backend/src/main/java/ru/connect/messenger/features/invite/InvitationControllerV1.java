package ru.connect.messenger.features.invite;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/invitations")
@PreAuthorize("hasRole('ADMIN')")
public class InvitationControllerV1 {
    private final InviteServiceImpl inviteService;

    @PostMapping
    public ResponseEntity<InvitationResponse> createToken() {
        var response = inviteService.create();
        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping("/count")
    public ResponseEntity<TotalInvitationsResponse> getTotalInvitations() {
        var response = inviteService.countInvitations();
        return ResponseEntity.ok()
                .body(response);
    }
}
