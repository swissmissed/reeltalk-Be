package com.alom.reeltalkbe.talk.controller;


import com.alom.reeltalkbe.common.response.BaseResponse;
import com.alom.reeltalkbe.talk.domain.TalkMessage;
import com.alom.reeltalkbe.talk.dto.TalkMessageDto;
import com.alom.reeltalkbe.talk.dto.TalkMessageResponseDto;
import com.alom.reeltalkbe.talk.service.TalkService;
import com.alom.reeltalkbe.user.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contents/{contentId}/talk")
public class TalkController {
    private final TalkService talkService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 websocket 으로 /app/chat/{roomId}/sendMessage 로 메시지를 발행하면
     * => @MessageMapping("/chat/{roomId}/sendMessage") 메서드가 받음
     * => 처리 후 /topic/chat/room/{roomId} 로 메시지를 전송

//    @MessageMapping("/chat/{roomId}/sendMessage")
//    @SendTo("/topic/chat/room/{roomId}")        //반환값이 이 경로 구독하는 모든 사용자에게 broadcast
//    public TalkMessage sendMessage(@DestinationVariable TalkMessageDto chatMessageDto) {
//        System.out.println("Message from" + chatMessageDto.getSender() + ", content" + chatMessageDto.getContent_id());
//        // DB 저장 + 반환된 객체를 /topic/chat/room/{roomId} 로 구독중인 모든 세션에게 broadcast
//        return talkService.saveMessage(chatMessageDto);
//    }
     */

    // rest api X, WebSocket 으로 메시지 보냈을때 broadcast 하는 메서드?
    @MessageMapping("/contents/{contentId}/talk")
    public void
    handleWebSocketMessage(@DestinationVariable Long contentId,
                                       @Payload TalkMessageDto talkMessageDto,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        talkMessageDto.setContentId(contentId);
        talkMessageDto.setUserId(userDetails.getUserId());
        TalkMessage savedMessage = talkService.saveTalkMessage(talkMessageDto);
        // WebSocket을 통해 실시간 전송
        messagingTemplate.convertAndSend("/topic/" + contentId + "/messages", savedMessage);
    }

    @GetMapping
    public BaseResponse<List<TalkMessageResponseDto>> getAllMessages(@PathVariable Long contentId) {
        return new BaseResponse<>(
                TalkMessageResponseDto.dtoListOf(
                        talkService.getMessagesByContentId(contentId)
                )
        );
    }

    @PostMapping
    public BaseResponse<TalkMessageResponseDto> sendMessage(@PathVariable Long contentId,
                            @RequestBody TalkMessageDto talkMessageDto,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        talkMessageDto.setContentId(contentId);
        talkMessageDto.setUserId(userDetails.getUserId());
        talkMessageDto.setSender(userDetails.getUsername());
        TalkMessage savedMessage = talkService.saveTalkMessage(talkMessageDto);
        // WebSocket을 통해 실시간 전송
        // 얘는 talkMessage save 할때 content, user 이 영속성이기 때문에 오류 안나는듯?
        messagingTemplate.convertAndSend("/topic/" + contentId + "/messages", savedMessage);
        return new BaseResponse<>(TalkMessageResponseDto.of(savedMessage));
    }

    @PutMapping("/{messageId}")
    public BaseResponse<TalkMessageResponseDto> updateMessage(@PathVariable Long contentId,
                                     @PathVariable Long messageId,
                                     @RequestBody TalkMessageDto updatedMessageDto,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        updatedMessageDto.setContentId(contentId);
        updatedMessageDto.setMessageId(messageId);
        updatedMessageDto.setUserId(userDetails.getUserId());
        TalkMessage updatedTalkMessage = talkService.updateTalkMessage(updatedMessageDto);

        // WebSocket을 통해 실시간 전송
        // talkMessage 의 content, user Lazy 로딩이 convertAndSend 메서드에 영향이 있다! (에러발생)
        // update 시에는 content 와 user 가 로딩이 안되어서 그런듯
        // messagingTemplate.convertAndSend("/topic/" + contentId + "/messages", updatedTalkMessage);
        return new BaseResponse<>(TalkMessageResponseDto.of(updatedTalkMessage));
    }

    @DeleteMapping("/{messageId}")
    public BaseResponse<Long> deleteMessage(@PathVariable Long contentId,
                              @PathVariable Long messageId,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        talkService.deleteTalkMessage(
                TalkMessageDto
                        .builder()
                        .messageId(messageId)
                        .userId(userDetails.getUserId())
                        .build()
        );
        // WebSocket을 통해 메시지 삭제 알림 전송
        messagingTemplate.convertAndSend("/topic/" + contentId + "/messages/delete", messageId);
        return new BaseResponse<>(messageId);
    }
}