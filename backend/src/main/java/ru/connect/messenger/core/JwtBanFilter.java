package ru.connect.messenger.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.connect.messenger.core.exception.UserDeletedException;
import ru.connect.messenger.features.user.service.UserBanService;
import ru.connect.messenger.features.user.service.UserDeletionService;

import java.io.IOException;

@Component
@Slf4j
public class JwtBanFilter extends OncePerRequestFilter {
    private final UserBanService userBanService;
    private final UserDeletionService userDeletionService;
    private final HandlerExceptionResolver resolver;

    public JwtBanFilter(UserBanService userBanService,
                        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
                        UserDeletionService userDeletionService) {
        this.userBanService = userBanService;
        this.userDeletionService = userDeletionService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            try {
                Long userId = Long.valueOf(jwtAuth.getToken().getClaimAsString("sub"));

                if (userBanService.isUserBanned(userId)) {
                    SecurityContextHolder.clearContext();

                    resolver.resolveException(request, response, null,
                            new DisabledException("Аккаунт заблокирован"));
                    return;
                }

                if (userDeletionService.isUserDeleted(userId)) {
                    SecurityContextHolder.clearContext();

                    resolver.resolveException(request, response, null,
                            new UserDeletedException("Аккаунт удален"));
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Не удалось распарсить User ID из токена (sub claim)", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
