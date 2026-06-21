package com.sbs.board.global.entity;

import com.sbs.board.user.dto.UserProfileResponse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User Entity와 1:1 관계를 갖는다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false, unique = true)
    private User user;

    @Column(name="phone_number", length = 30)
    private String phoneNumber;

    private LocalDate birth;

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at")
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static UserProfileResponse toDTO(UserProfile user) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.setNickName(user.getUser().getNickName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBirth(user.getBirth() != null ? user.getBirth().toString():"");
        dto.setCreatedAt(user.getCreatedAt().toString());

        return dto;
    }
}

// Refresh Token: 만료시간 5시간, Access Token 재발급 용도로만 사용
// Access Token: 15분