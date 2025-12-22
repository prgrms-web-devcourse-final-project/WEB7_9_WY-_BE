package back.kalender.global.initData.user;

import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
@Order(0)
public class UserBaseInitData {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (userRepository.count() >= 6) {
            log.info("User base data already initialized");
            return;
        }
        createUsers();
    }

    private void createUsers() {
        for (int i = 1; i <= 6; i++) {
            String email = "user" + i + "@test.com";

            if (userRepository.findByEmail(email).isPresent()) {
                continue;
            }

            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("password123!"))
                    .nickname("테스트유저" + i)
                    .level(1)
                    .emailVerified(true)
                    .build();

            userRepository.save(user);
        }

        log.info("User base data initialized (6 users)");
    }
}