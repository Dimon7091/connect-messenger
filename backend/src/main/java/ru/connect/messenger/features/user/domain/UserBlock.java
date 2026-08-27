package ru.connect.messenger.features.user.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "user_block")
@IdClass(BlockId.class)
public class UserBlock {
    @Id
    private Long blockerId;

    @Id
    private Long blockedId;
}

