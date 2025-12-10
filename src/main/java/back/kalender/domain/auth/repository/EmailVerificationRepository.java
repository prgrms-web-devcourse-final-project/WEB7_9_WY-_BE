package back.kalender.domain.auth.repository;

import back.kalender.domain.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<EmailVerification> findByCode(String code);
    void deleteByUserId(Long userId);
}