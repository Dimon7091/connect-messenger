package ru.connect.messenger.features.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.connect.messenger.features.user.repository.UserRepository;
@AllArgsConstructor
@Component
public class UniqueUserNameValidator implements ConstraintValidator<UniqueUserName, String> {
    private final UserRepository userRepository;

    @Override
    public boolean isValid(String userName, ConstraintValidatorContext context) {
        if (userName == null || userName.isBlank()) {
            return true; // Пусть @NotBlank занимается проверкой на пустоту
        }
        // Если имя НЕ существует, валидация успешна (true)
        return !userRepository.existsByUserName(userName);
    }
}

