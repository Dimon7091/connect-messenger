package ru.gorbunov.connect.web.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import ru.gorbunov.connect.core.models.User;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    @Autowired
    private JwtEncoder encoder;

    public String generateToken(User user) {
        // Текущее время в UTC
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        String roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", "")) // Убираем префикс, если он есть в БД
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now.toInstant())
                .expiresAt(now.plusMonths(1).toInstant())  // ✅ +1 месяц
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
