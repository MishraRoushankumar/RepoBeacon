package com.repobeacon.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;

@Configuration
public class GoogleGenAiEmbeddingConfig {

  @Bean
  EmbeddingModel googleGenAiEmbeddingModel(
      Client client,
      @Value("${spring.ai.google.genai.embedding.text.model:gemini-embedding-001}") String model) {
    return new EmbeddingModel() {
      @Override
      public EmbeddingResponse call(EmbeddingRequest request) {
        String selectedModel = (request.getOptions() != null && request.getOptions().getModel() != null)
            ? request.getOptions().getModel()
            : model;
        var response = client.models.embedContent(
            selectedModel,
            request.getInstructions(),
            EmbedContentConfig.builder().outputDimensionality(1536).build());
        var vectors = response.embeddings()
            .orElseThrow(() -> new IllegalStateException("Google GenAI returned no embeddings"));
        List<Embedding> embeddings = new ArrayList<>(vectors.size());
        for (int index = 0; index < vectors.size(); index++) {
          List<Float> values = vectors.get(index).values()
              .orElseThrow(() -> new IllegalStateException("Google GenAI returned an empty embedding"));
          float[] vector = new float[values.size()];
          for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            vector[valueIndex] = values.get(valueIndex);
          }
          embeddings.add(new Embedding(vector, index));
        }
        return new EmbeddingResponse(embeddings);
      }

      @Override
      public float[] embed(Document document) {
        return embed(getEmbeddingContent(document));
      }

      @Override
      public int dimensions() {
        return 1536;
      }
    };
  }
}