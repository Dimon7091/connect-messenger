package ru.gorbunov.connect.core.dto.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.gorbunov.connect.core.repository.UserRepository;

@Component
public class UniqueUserNameValidator implements ConstraintValidator<UniqueUserName, String> {

    private final UserRepository userRepository;

    public UniqueUserNameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String userName, ConstraintValidatorContext context) {
        if (userName == null || userName.isBlank()) {
            return true; // Пусть @NotBlank занимается проверкой на пустоту
        }
        // Если имя НЕ существует, валидация успешна (true)
        return !userRepository.existsByUserName(userName);
    }
}

