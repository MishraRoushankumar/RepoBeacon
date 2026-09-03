package com.repobeacon.backend.services.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.repobeacon.backend.repository.ChatMessageRepository;

class ChatStreamHandlerTest {

  private ChatStreamHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ChatStreamHandler(
        mock(ChatModel.class),
        mock(ChatMessageRepository.class),
        mock(CitationMapper.class));
  }

  @Test
  void handleStreamError_sendsStableErrorMessageAndHidesInternalExceptionDetails() throws Exception {
    SseEmitter emitter = mock(SseEmitter.class);

    handler.handleStreamError(emitter, new RuntimeException("Sensitive secret internal exception message"));

    ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
    verify(emitter).send(captor.capture());

    Set<ResponseBodyEmitter.DataWithMediaType> dataSet = captor.getValue().build();
    String formatted = dataSet.stream()
        .map(d -> String.valueOf(d.getData()))
        .reduce("", (a, b) -> a + b);

    assertThat(formatted).contains("event:error");
    assertThat(formatted).contains("Error generating reply");
    assertThat(formatted).doesNotContain("Sensitive secret internal exception message");
  }
}
