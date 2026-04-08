package ru.gorbunov.connect.web.controller.api.v1;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.dto.UserPatchUpdateRequest;
import ru.gorbunov.connect.core.dto.UserPutUpdateRequest;
import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.dto.UserStatResponse;
import ru.gorbunov.connect.core.models.Role;
import ru.gorbunov.connect.core.models.User;
import ru.gorbunov.connect.core.repository.UserRepository;
import ru.gorbunov.connect.core.service.UserService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {

    private final UserService userService;

    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest requestData) {
        var savedUser = userService.create(requestData, Role.ROLE_USER);
        return ResponseEntity.created(URI.create("api/v1/users/" + savedUser.id()))
                .body(savedUser);
    }

    @GetMapping("")
    public ResponseEntity<Page<UserResponse>> index(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        var users = userService.findAll(pageable);
        return ResponseEntity.ok()
                .body(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> show(@PathVariable("id") Long id) {
        var user = userService.findById(id);
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        var user = userService.findByUserName(userDetails.getUsername());
        return ResponseEntity.ok()
                .body(user);
    }

    @GetMapping("/stat")
    public UserStatResponse stat() {
       return userService.getUsersStat();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> putUpdate(
            @PathVariable("id") Long id,
            @RequestBody UserPutUpdateRequest requestData
    ) {
        var updatedUser = userService.putUpdate(id, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patchUpdate(
            @PathVariable("id") Long id,
            @RequestBody UserPatchUpdateRequest requestData
    ) {
        var updatedUser = userService.patchUpdate(id, requestData);
        return ResponseEntity.ok()
                .body(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        userService.delete(id);
    }
}
