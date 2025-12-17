package back.kalender.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 메시지 전송 요청")
public record SendMessageRequest(

        @Schema(description = "메시지 내용", example = "안녕하세요! 같이 공연 가요~", required = true)
        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다")
        String message
) {}