package ru.gorbunov.connect.core.service;

import jakarta.transaction.Transactional;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import ru.gorbunov.connect.core.dto.UserCreateRequest;
import ru.gorbunov.connect.core.dto.UserResponse;
import ru.gorbunov.connect.core.exception.EmailAlreadyExistsException;
import ru.gorbunov.connect.core.exception.UserNameAlreadyExistsException;
import ru.gorbunov.connect.core.mapper.UserMapper;
import ru.gorbunov.connect.core.repository.UserRepository;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper mapper;

    // --- Create ---
    public UserResponse create(UserCreateRequest requestData) {
        if (userRepository.existByEmail(requestData.email())) {
            throw new EmailAlreadyExistsException(
                    "Пользователь с email: " + requestData.email() + " уже существует"
            );
        }

        if (userRepository.existByUserName(requestData.userName())) {
            throw new UserNameAlreadyExistsException(
                    "Пользователь с именем пользователя: " + requestData.userName() + " уже существует"
            );
        }

        var user = mapper.toEntity(requestData);
        var savedUser = userRepository.save(user);
        return mapper.toDto(savedUser);
    }
}
