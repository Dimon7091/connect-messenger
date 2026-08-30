package ru.connect.messenger.features.invite;

public interface InviteService {
    InvitationResponse create();
    TotalInvitationsResponse countInvitations();
    void validateAndConsumeInvitation(String token);
    void cronCleanExpired();
    String generateUrl(String token);
}