package ru.gorbunov.connect.core.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.gorbunov.connect.core.exception.ConflictException;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.IllegalActionException;
import ru.gorbunov.connect.core.exception.ResourceAlreadyExistsException;
import ru.gorbunov.connect.core.exception.ResourceNotFoundException;
import ru.gorbunov.connect.core.exception.UserDeletedException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ошибка валидации данных");
        response.put("errors", errors);
        return ResponseEntity.unprocessableEntity().body(response);
    }

    @ExceptionHandler
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        // Теперь вместо ошибки в логах клиент получит 403
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Не достаточно прав для доступа");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Access Denied",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Account disabled",
                        "message", ex.getMessage(),
                        "action", "LOGOUT_AND_REDIRECT",
                        "errorCode", "AUTH_ACCOUNT_BANNED"
                ));
    }

    @ExceptionHandler(IllegalActionException.class)
    public ResponseEntity<Map<String, String>> handleIllegalAction(IllegalActionException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Illegal action",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(UserDeletedException.class)
    public ResponseEntity<Map<String, String>> handleIllegalAction(UserDeletedException ex) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(Map.of(
                        "error", "User deleted",
                        "message", ex.getMessage(),
                        "action", "LOGOUT_AND_REDIRECT",
                        "errorCode", "USER_ACCOUNT_DELETED"
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleIllegalAction() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", "Неверный логин или пароль"
                ));
    }
}
