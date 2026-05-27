package ru.gorbunov.connect.core.models;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter

@Entity
@Table(name = "profile")
@EntityListeners(AuditingEntityListener.class)
public class Profile {
    @Id
    @Tsid
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @Column(nullable = true, name = "avatar_url")
    private String avatarUrl;
}
