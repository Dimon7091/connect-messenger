package ru.gorbunov.connect.web.controller.api.v1;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.web.dto.AuthRequest;
import ru.gorbunov.connect.web.dto.AuthResponse;
import ru.gorbunov.connect.web.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/api/v1/auth")
public class AuthControllerV1 {

    private static final Log log = LogFactory.getLog(AuthControllerV1.class);
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createJwtForUser(
            @RequestBody AuthRequest authRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // СОХРАНЯЕМ результат аутентификации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );
        // Теперь в authentication есть principal!
        User user = (User) authentication.getPrincipal();
        UserResponse currentUser = userService.findByUserName(user.getUsername());
        // Генерируем токен
        String token = jwtUtils.generateToken(user);
        var response = new AuthResponse(currentUser, token);
        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/login-admin")
    public ResponseEntity<Map<String, String>> creteJwtForAdmin(@RequestBody AuthRequest authRequest) {
        // Проверка роли admin
        var adminDetails = userService.findByUserName(authRequest.username());
        boolean isAdmin = adminDetails.getRoles().stream()
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Доступ запрещен"));
        }
        // СОХРАНЯЕМ результат аутентификации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );
        // Теперь в authentication есть principal!
        User admin = (User) authentication.getPrincipal();
        // Генерируем токен
        String token = jwtUtils.generateToken(admin);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok()
                .body(response);
    }



    @PostMapping("verify-admin-token")
    @PreAuthorize("hasRole('ADMIN')") // Spring сам вернет 403, если роли нет
    public ResponseEntity<Void> verifyAdminToken() {
        return ResponseEntity.ok().build();
    }
}
