package ru.gorbunov.connect.web.controller.api.v1;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.web.controller.dto.AuthRequest;
import ru.gorbunov.connect.web.controller.util.JwtUtil;

@RestController()
@RequestMapping("/api/v1")
public class AuthControllerV1 {

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public String createJWT(@RequestBody AuthRequest authRequest) {
        // СОХРАНЯЕМ результат аутентификации
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.userName(),
                        authRequest.password()
                )
        );
        // Теперь в authentication есть principal!
        User user = (User) authentication.getPrincipal();

        // Генерируем токен
        return jwtUtils.generateToken(user);
    }

}
