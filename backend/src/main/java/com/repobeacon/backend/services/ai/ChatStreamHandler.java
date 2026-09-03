package com.repobeacon.backend.services.ai;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.repobeacon.backend.dto.ChatMessageResponse;
import com.repobeacon.backend.dto.CitationDto;
import com.repobeacon.backend.entity.ChatMessage;
import com.repobeacon.backend.entity.MessageRole;
import com.repobeacon.backend.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generation step: call Google GenAI via Spring AI and stream tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamHandler {

  private final ChatModel chatModel;
  private final ChatMessageRepository chatMessageRepository;
  private final CitationMapper citationMapper;

  public SseEmitter stream(
      UUID sessionId,
      ChatMessageResponse savedUserMessage,
      List<CitationDto> citations,
      String systemPrompt,
      String userPrompt) {

    SseEmitter emitter = new SseEmitter(RagSettings.STREAM_TIMEOUT_MS);
    StringBuilder fullReply = new StringBuilder();

    try {
      emitter.send(SseEmitter
          .event()
          .name("user_message")
          .data(savedUserMessage));

      ChatClient.builder(chatModel)
          .build()
          .prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .stream()
          .content()
          .doOnNext(token -> appendToken(emitter, fullReply, token))
          .doOnComplete(() -> completeStream(emitter, sessionId, fullReply, citations))
          .subscribe(
              token -> {},
              err -> handleStreamError(emitter, err)
          );

    } catch (Exception e) {
      handleStreamError(emitter, e);
    }

    return emitter;
  }

  private void appendToken(SseEmitter emitter, StringBuilder fullReply, String token) {
    fullReply.append(token);
    try {
      emitter.send(SseEmitter.event()
          .name("token")
          .data(token, MediaType.APPLICATION_JSON));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void completeStream(
      SseEmitter emitter,
      UUID sessionId,
      StringBuilder fullReply,
      List<CitationDto> citations) {
    try {
      ChatMessage assistant = chatMessageRepository.save(ChatMessage.builder()
          .sessionId(sessionId)
          .role(MessageRole.ASSISTANT)
          .content(fullReply.toString())
          .citations(citationMapper.toJson(citations))
          .build());

      emitter.send(SseEmitter.event()
          .name("assistant_message")
          .data(toMessageResponse(assistant)));
      emitter.send(SseEmitter.event().name("done").data("[DONE]"));
      emitter.complete();
    } catch (Exception ex) {
      handleStreamError(emitter, ex);
    }
  }

  private void handleStreamError(SseEmitter emitter, Throwable err) {
    if (err instanceof org.springframework.web.context.request.async.AsyncRequestNotUsableException || err.getCause() instanceof java.io.IOException) {
      log.debug("Client disconnected during chat stream: {}", err.getMessage());
    } else {
      log.error("Chat stream error", err);
    }
    try {
      String errorMessage = err.getMessage() != null ? err.getMessage() : "Error generating reply";
      emitter.send(SseEmitter.event()
          .name("error")
          .data(java.util.Map.of("message", errorMessage), MediaType.APPLICATION_JSON));
    } catch (Exception ex) {
      log.debug("Failed to send SSE error event", ex);
    } finally {
      try {
        emitter.complete();
      } catch (Exception ex) {
        log.debug("Failed to complete SSE emitter", ex);
      }
    }
  }

  private ChatMessageResponse toMessageResponse(ChatMessage message) {
    return new ChatMessageResponse(
        message.getId(),
        message.getRole(),
        message.getContent(),
        citationMapper.fromJson(message.getCitations()),
        message.getCreatedAt());
  }

}
