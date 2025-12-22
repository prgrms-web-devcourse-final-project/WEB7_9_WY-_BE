package back.kalender.domain.chat.controller;

import back.kalender.domain.chat.dto.request.SendMessageRequest;
import back.kalender.domain.chat.dto.response.ChatMessageResponse;
import back.kalender.domain.chat.dto.response.KickMemberResponse;
import back.kalender.domain.chat.dto.response.LeaveRoomResponse;
import back.kalender.domain.chat.dto.response.RoomJoinedResponse;
import back.kalender.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Tag(name = "Chat", description = "채팅 관련 API")
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.join/{partyId}")
    public void joinRoom(
            @DestinationVariable Long partyId,
            Principal principal
    ) {
        RoomJoinedResponse response = chatService.joinRoom(partyId, principal.getName());
        messagingTemplate.convertAndSend("/topic/room/" + partyId, response);
    }

    @MessageMapping("/chat.send/{partyId}")
    public void sendMessage(
            @DestinationVariable Long partyId,
            @Payload @Valid SendMessageRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatService.sendMessage(
                partyId, request, principal.getName());
        messagingTemplate.convertAndSend("/topic/room/" + partyId, response);
    }

    @MessageMapping("/chat.leave/{partyId}")
    public void leaveRoom(
            @DestinationVariable Long partyId,
            Principal principal
    ) {
        LeaveRoomResponse response = chatService.leaveRoom(partyId, principal.getName());
        messagingTemplate.convertAndSend("/topic/room/" + partyId, response);
    }

    @MessageMapping("/chat.kick/{partyId}/{targetMemberId}")
    public void kickMember(
            @DestinationVariable Long partyId,
            @DestinationVariable Long targetMemberId,
            Principal principal
    ) {
        KickMemberResponse response = chatService.kickMember(
                partyId, targetMemberId, principal.getName());
        messagingTemplate.convertAndSend("/topic/room/" + partyId, response);
    }
}