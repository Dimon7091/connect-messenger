package ru.connect.messenger.features.user.specifications;

import org.springframework.data.jpa.domain.Specification;
import ru.connect.messenger.features.user.domain.User;


public class UserSpecification {
    public static Specification<User> hasUserName(String userName) {
        return (root, query, criteriaBuilder) -> {
            if (userName == null || userName.trim().isEmpty()) {
                return null;
            }
            // Поиск без учета регистра (LIKE %имя%)
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("userName")),
                    "%" + userName.toLowerCase() + "%"
            );
        };
    }
}
