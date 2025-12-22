package back.kalender.domain.chat.controller;

import back.kalender.domain.chat.dto.response.ChatHistoryResponse;
import back.kalender.domain.chat.dto.response.ChatRoomInfoResponse;
import back.kalender.domain.chat.dto.response.MyChatRoomsResponse;
import back.kalender.domain.chat.dto.response.ParticipantListResponse;
import back.kalender.domain.chat.service.ChatRoomService;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat Room", description = "채팅방 관리 API")
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Validated
public class ChatRoomController implements ChatRoomControllerSpec {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ResponseEntity<MyChatRoomsResponse> getMyChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MyChatRoomsResponse response = chatRoomService.getMyChatRooms(
                userDetails.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{partyId}")
    public ResponseEntity<ChatRoomInfoResponse> getChatRoomInfo(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatRoomInfoResponse response = chatRoomService.getChatRoomInfo(
                partyId, userDetails.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{partyId}/participants")
    public ResponseEntity<ParticipantListResponse> getParticipants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ParticipantListResponse response = chatRoomService.getParticipants(
                partyId, userDetails.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{partyId}/messages")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable Long partyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatHistoryResponse response = chatRoomService.getChatHistory(
                partyId, page, size, userDetails.getEmail());
        return ResponseEntity.ok(response);
    }
}