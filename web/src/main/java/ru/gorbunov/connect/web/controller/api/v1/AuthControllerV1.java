package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.user.UserCreateRequest;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.service.InviteService;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.core.service.orchestrators.UserProviderService;
import ru.gorbunov.connect.web.dto.AuthRequest;
import ru.gorbunov.connect.web.dto.AuthResponse;
import ru.gorbunov.connect.web.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/api/v1/auth")
public class AuthControllerV1 {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserProviderService userProviderService;

    @Autowired
    private InviteService inviteService;

    @Autowired
    private UserMapper mapper;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createJwtForUser(
            @RequestBody AuthRequest authRequest
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
        UserResponse currentUser = userProviderService.getUserDetails(user.getId());
        // Генерируем токен
        String token = jwtUtils.generateToken(user);
        var response = new AuthResponse(currentUser, token);
        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody @Valid UserCreateRequest request) {
        inviteService.validateAndConsumeInvitation(request.invitationToken());
        var newUser = userService.create(request, Role.ROLE_USER);
        String token = jwtUtils.generateToken(newUser);
        var userResponse = mapper.toDto(newUser);
        return ResponseEntity.ok()
                .body(new AuthResponse(userResponse, token));
    }

    @PostMapping("/login-admin")
    public ResponseEntity<Map<String, String>> creteJwtForAdmin(@RequestBody AuthRequest authRequest) {
        // Проверка роли admin
        boolean isAdmin = userService.isAdmin(authRequest.username());
        if (!isAdmin) {
            throw new AccessDeniedException("Доступ запрещен");
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
