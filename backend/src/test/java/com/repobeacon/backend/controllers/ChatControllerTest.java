package com.repobeacon.backend.controllers;

import com.repobeacon.backend.dto.ChatMessageRequest;
import com.repobeacon.backend.entity.User;
import com.repobeacon.backend.exceptions.BadRequestException;
import com.repobeacon.backend.exceptions.GlobalExceptionHandler;
import com.repobeacon.backend.exceptions.NotFoundException;
import com.repobeacon.backend.exceptions.UnauthorizedException;
import com.repobeacon.backend.security.AppUserPrincipal;
import com.repobeacon.backend.security.CurrentUser;
import com.repobeacon.backend.services.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  @Mock
  private CurrentUser currentUser;

  @Mock
  private ChatService chatService;

  @InjectMocks
  private ChatController chatController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(chatController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    when(currentUser.require()).thenReturn(new AppUserPrincipal(user, Map.of()));
  }

  @Test
  void sendMessage_whenBadRequestException_returnsJsonError() throws Exception {
    UUID sessionId = UUID.randomUUID();
    when(chatService.streamReply(any(), eq(sessionId), any()))
        .thenThrow(new BadRequestException("Repository is not ready for chat"));

    mockMvc.perform(post("/api/chat/sessions/" + sessionId + "/messages")
            .requestAttr(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(MediaType.TEXT_EVENT_STREAM))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Hello\"}")
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Repository is not ready for chat"));
  }

  @Test
  void sendMessage_whenNotFoundException_returnsJsonError() throws Exception {
    UUID sessionId = UUID.randomUUID();
    when(chatService.streamReply(any(), eq(sessionId), any()))
        .thenThrow(new NotFoundException("Chat session not found"));

    mockMvc.perform(post("/api/chat/sessions/" + sessionId + "/messages")
            .requestAttr(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(MediaType.TEXT_EVENT_STREAM))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Hello\"}")
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Chat session not found"));
  }

  @Test
  void sendMessage_whenGenericException_returnsJsonError() throws Exception {
    UUID sessionId = UUID.randomUUID();
    when(chatService.streamReply(any(), eq(sessionId), any()))
        .thenThrow(new RuntimeException("Something unexpected went wrong"));

    mockMvc.perform(post("/api/chat/sessions/" + sessionId + "/messages")
            .requestAttr(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Set.of(MediaType.TEXT_EVENT_STREAM))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Hello\"}")
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").value("Something unexpected went wrong"));
  }
}
