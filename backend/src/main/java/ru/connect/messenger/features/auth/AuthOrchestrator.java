package ru.connect.messenger.features.auth;

import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.connect.messenger.core.JwtTokenProvider;
import ru.connect.messenger.core.SecurityUser;
import ru.connect.messenger.features.invite.InviteService;
import ru.connect.messenger.features.user.api.UserProviderService;
import ru.connect.messenger.features.user.api.UserService;
import ru.connect.messenger.features.user.domain.Role;
import ru.connect.messenger.features.user.domain.User;
import ru.connect.messenger.features.user.dto.UserCreateRequest;
import ru.connect.messenger.features.user.dto.UserPrivateResponse;
import ru.connect.messenger.features.user.mapper.UserMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class AuthOrchestrator {
    private final UserService userService;
    private final JwtTokenProvider jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserProviderService userProviderService;
    private final InviteService inviteService;
    private final UserMapper mapper;

    public AuthResponse authenticateUser(AuthRequest authRequest) {
        // СОХРАНЯЕМ результат аутентификации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username().toLowerCase(),
                        authRequest.password()
                )
        );

        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        UserPrivateResponse currentUser = userProviderService.getUserDetailsForAuth(user.getId());
        // Генерируем токен
        String token = jwtUtils.generateToken(user);
        return new AuthResponse(currentUser, token);
    }

    public AuthResponse register(UserCreateRequest request) {
        inviteService.validateAndConsumeInvitation(request.invitationToken());
        User newUser = userService.create(request, Role.ROLE_USER);
        SecurityUser securityUser = new SecurityUser(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getPassword(),
                newUser.getRoles().toString()
        );
        String token = jwtUtils.generateToken(securityUser);
        var userResponse = mapper.toPrivateDto(newUser);
        return new AuthResponse(userResponse, token);
    }

    public Map<String, String> authenticateAdmin(AuthRequest authRequest) {
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
        SecurityUser admin = (SecurityUser) authentication.getPrincipal();
        // Генерируем токен
        String token = jwtUtils.generateToken(admin);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }

}
