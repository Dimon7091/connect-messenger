package ru.gorbunov.connect.web.controller.api.v1;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.service.UserService;
import ru.gorbunov.connect.web.dto.AuthRequest;
import ru.gorbunov.connect.web.util.JwtUtil;

@RestController()
@RequestMapping("/api/v1/auth")
public class AuthControllerV1 {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public String createJwtForUser(@RequestBody AuthRequest authRequest) {
        // СОХРАНЯЕМ результат аутентификации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );
        // Теперь в authentication есть principal!
        User user = (User) authentication.getPrincipal();
        // Генерируем токен
        return jwtUtils.generateToken(user);
    }

    @PostMapping("/login-admin")
    public ResponseEntity<String> creteJwtForAdmin(@RequestBody AuthRequest authRequest) {
        // Проверка роли admin
        var adminDetails = userService.findByUserName(authRequest.username());
        boolean isAdmin = adminDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Доступ запрещен");
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
        return ResponseEntity.ok()
                .body(jwtUtils.generateToken(admin));
    }

    @PostMapping("verify-admin-token")
    public ResponseEntity<Void> verifyAdminToken(@AuthenticationPrincipal UserDetails userDetail) {
        boolean isAdmin = userDetail.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            ResponseEntity.status(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok().build();
    }
}
