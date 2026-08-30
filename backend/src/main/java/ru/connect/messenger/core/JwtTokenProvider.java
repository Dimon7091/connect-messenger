package ru.connect.messenger.core;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class JwtTokenProvider {
    private JwtEncoder encoder;

    public String generateToken(SecurityUser user) {
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
                .subject(String.valueOf(user.getId()))
                .claim("preferred_username", user.getUsername())
                .claim("roles", roles)
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
