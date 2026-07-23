package ru.gorbunov.connect.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gorbunov.connect.core.dto.Invitation.TotalInvitationsResponse;
import ru.gorbunov.connect.core.dto.Invitation.InvitationResponse;
import ru.gorbunov.connect.core.exception.InvitationIsNotValidException;
import ru.gorbunov.connect.core.models.Invitation;
import ru.gorbunov.connect.core.repository.InvitationRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InviteService {
    private final InvitationRepository invitationRepository;
    private final String appUrl;

    public InviteService(
            InvitationRepository invitationRepository,
            @Value("${app.url}") String appUrl
    ) {
        this.invitationRepository = invitationRepository;
        this.appUrl = appUrl;
    }

    @Transactional
    public InvitationResponse create() {
        var invitation = new Invitation();
        String token = UUID.randomUUID().toString();
        invitation.setToken(token);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        invitationRepository.save(invitation);
        return new InvitationResponse(generateUrl(token));
    }

    public TotalInvitationsResponse countInvitations() {
        return new TotalInvitationsResponse(
                invitationRepository.count()
        );
    }

    public void validateAndConsumeInvitation(String token) {
        var invitation = invitationRepository
                .findInvitationsByToken(token)
                .orElseThrow(InvitationIsNotValidException::new);
        invitationRepository.delete(invitation);
    }

    /**
     * Автоматическая очистка базы данных от просроченных инвайтов.
     * Запускается каждый час (в 00 минут каждого часа).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cronCleanExpired() {
        invitationRepository.deleteExpired(LocalDateTime.now());
    }

    // Всопогательный метод для генерации готовой ссылки приглашения
    public String generateUrl(String token) {
        String normalizeUrl = appUrl.strip();
        String query = "?open=register&invitationToken=" + token;
        if (normalizeUrl.endsWith("/")) {
            normalizeUrl = normalizeUrl.substring(0, normalizeUrl.length() - 1);
        }
        return normalizeUrl + "/login" + query;
    }
}
