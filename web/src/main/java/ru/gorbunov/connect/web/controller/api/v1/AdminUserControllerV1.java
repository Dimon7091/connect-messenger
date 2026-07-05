package ru.gorbunov.connect.web.controller.api.v1;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.user.UserIsBannedUpdateRequest;
import ru.gorbunov.connect.core.dto.user.UserAdminResponse;
import ru.gorbunov.connect.core.dto.user.UserStatResponse;
import ru.gorbunov.connect.core.exception.IllegalActionException;
import ru.gorbunov.connect.core.service.BanService;
import ru.gorbunov.connect.core.service.orchestrators.UserDeletionService;
import ru.gorbunov.connect.core.service.orchestrators.UserProviderService;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminUserControllerV1 {
    private final UserProviderService userProviderService;
    private final BanService banService;
    private final UserDeletionService userDeletionService;

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserAdminResponse> show(@PathVariable("id") Long id) {
        var user = userProviderService.getUserDetailsForAdmin(id);
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserAdminResponse>> index(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "sortBy", defaultValue = "userName", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir
    ) {
        var users = userProviderService.findAllUsersDetailsWithPagination(
                page,
                size,
                userName,
                sortBy,
                sortDir
        );
        return ResponseEntity.ok()
                .body(users);
    }

    @GetMapping("/stat")
    public ResponseEntity<UserStatResponse> stat() {
        var response = userProviderService.getUsersStat();
        return ResponseEntity.ok()
                .body(response);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<UserAdminResponse> block(
            @PathVariable("id") Long userId,
            @RequestBody UserIsBannedUpdateRequest request,
            @AuthenticationPrincipal Jwt token
    ) {
        Long currentAdminId = Long.parseLong(token.getClaim("sub"));
        if (currentAdminId.equals(userId)) {
            throw new IllegalActionException("Вы не можете заблокировать собственный аккаунт.");
        }
        banService.toggleUserBlockStatus(userId, request.isBanned());
        var response = userProviderService.getUserDetailsForAdmin(userId);
        return ResponseEntity.ok()
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserAdminResponse> softDelete(@PathVariable("id") Long userId, @AuthenticationPrincipal Jwt token) {
        Long currentAdminId = Long.parseLong(token.getClaim("sub"));
        var response = userDeletionService.delete(userId, currentAdminId);
        return ResponseEntity.ok()
                .body(response);
    }
}
