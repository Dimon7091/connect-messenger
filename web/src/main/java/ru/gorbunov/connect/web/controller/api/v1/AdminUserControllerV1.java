package ru.gorbunov.connect.web.controller.api.v1;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.user.UserAdminResponse;
import ru.gorbunov.connect.core.dto.user.UserResponse;
import ru.gorbunov.connect.core.dto.user.UserStatResponse;
import ru.gorbunov.connect.core.service.orchestrators.UserProviderService;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserControllerV1 {
    public AdminUserControllerV1(UserProviderService userProviderService) {
        this.userProviderService = userProviderService;
    }

    private final UserProviderService userProviderService;

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserResponse> show(@PathVariable("id") Long id) {
        var user = userProviderService.getUserDetails(id);
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("")
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
}
