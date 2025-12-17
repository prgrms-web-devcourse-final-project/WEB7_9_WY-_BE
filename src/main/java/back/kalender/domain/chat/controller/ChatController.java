package back.kalender.domain.chat.controller;

import back.kalender.domain.chat.dto.request.SendMessageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Validated
public class ChatController{

    @MessageMapping("/chat.join/{partyId}")
    public void joinRoom(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @DestinationVariable Long partyId,

            Principal principal
    ) {

    }


    @MessageMapping("/chat.send/{partyId}")
    public void sendMessage(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @DestinationVariable Long partyId,

            @Parameter(description = "메시지 내용", required = true)
            @Payload SendMessageRequest request,

            Principal principal
    ){

    }


    @MessageMapping("/chat.leave/{partyId}")
    public void leaveRoom(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @DestinationVariable Long partyId,

            Principal principal
    ){

    }


    @MessageMapping("/chat.kick/{partyId}/{targetMemberId}")
    public void kickMember(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @DestinationVariable Long partyId,

            @Parameter(description = "강퇴할 멤버 ID", required = true, example = "3")
            @DestinationVariable Long targetMemberId,

            Principal principal
    ){

    }
}
