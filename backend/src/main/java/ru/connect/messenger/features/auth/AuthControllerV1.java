package ru.connect.messenger.features.auth;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.orchestrator.AuthOrchestrator;

import java.net.URISyntaxException;
import java.util.Map;

@AllArgsConstructor
@RestController()
@RequestMapping("/api/v1/auth")
public class AuthControllerV1 {
    private final AuthOrchestrator authOrchestrator;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(
            @RequestBody @Valid AuthRequest authRequest
    ) {
        var response = authOrchestrator.authenticateUser(authRequest);
        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/login-admin")
    public ResponseEntity<Map<String, String>> authenticateAdmin(@RequestBody AuthRequest authRequest) {
        var response = authOrchestrator.authenticateAdmin(authRequest);
        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid UserCreateRequest request
    ) throws URISyntaxException {
        var response = authOrchestrator.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("verify-admin-token")
    @PreAuthorize("hasRole('ADMIN')") // Spring сам вернет 403, если роли нет
    public ResponseEntity<Void> verifyAdminToken() {
        return ResponseEntity.ok().build();
    }
}
