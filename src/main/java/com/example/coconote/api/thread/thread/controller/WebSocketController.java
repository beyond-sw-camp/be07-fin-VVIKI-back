package com.example.coconote.api.thread.thread.controller;

import com.example.coconote.api.member.entity.Member;
import com.example.coconote.api.member.repository.MemberRepository;
import com.example.coconote.security.token.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.coconote.api.thread.thread.entity.MessageType;
import com.example.coconote.api.thread.thread.dto.requset.ThreadReqDto;
import com.example.coconote.api.thread.thread.dto.response.ThreadResDto;
import com.example.coconote.api.thread.thread.service.ThreadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WebSocketController {
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private final SimpMessageSendingOperations messagingTemplate;
    private final ThreadService threadService;
//    private final KafkaTemplate<String, ThreadReqDto> kafkaThreadTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @MessageMapping("/chat/message")
    public void message(ThreadReqDto message, @Header("Authorization") String token) {
        String email = jwtTokenProvider.getEmailFromToken(token);
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("이메일에 해당하는 멤버가 없습니다."));
        message.setSenderId(member.getId());
        if (MessageType.ENTER.equals(message.getType()))
            message.setContent(member.getId() + "님이 입장하셨습니다.");

        kafkaTemplate.send("chat_topic", message.getChannelId().toString(), message);

//        kafka 안거치고 바로 보낼때 사용
//        ThreadResDto threadResDto = threadService.createThread(message);
//        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getChannelId(), threadResDto);
    }

    @KafkaListener(topics = "chat_topic", groupId = "websocket-group")
    public void listen(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        ThreadReqDto threadReqDto;

        try {
            threadReqDto = objectMapper.readValue(message, ThreadReqDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ThreadResDto threadResDto;

        if(MessageType.UPDATE.equals(threadReqDto.getType())) {
            threadResDto = threadService.updateThread(threadReqDto);
        } else if (MessageType.DELETE.equals(threadReqDto.getType())) {
            threadResDto = threadService.deleteThread(threadReqDto.getThreadId());
        } else {
            threadResDto = threadService.createThread(threadReqDto,threadReqDto.getSenderId());
        }

        // 수신한 메시지를 채널로 브로드캐스트하기 전에 로그 찍기
        log.info("Received message from Kafka: {}", threadResDto);
        // 수신한 메시지를 채널로 브로드캐스트
        messagingTemplate.convertAndSend("/sub/chat/room/" + threadReqDto.getChannelId(), threadResDto);
    }
}