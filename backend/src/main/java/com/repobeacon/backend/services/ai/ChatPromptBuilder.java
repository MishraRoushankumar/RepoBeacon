package com.repobeacon.backend.services.ai;

import org.springframework.stereotype.Component;

/**
 * Builds the prompts sent to Google GenAI.
 *
 * <p>
 * We use two messages:
 * <ul>
 * <li><b>System</b> — rules for how the assistant should behave</li>
 * <li><b>User</b> — retrieved code context + the actual question</li>
 * </ul>
 */
@Component
public class ChatPromptBuilder {

  public String systemPrompt(String repositoryFullName) {
    return """
        You are RepoBeacon, an expert AI assistant for the %s codebase.

                Your job is to help developers understand and work with this repository.

                ## Rules

                - Answer questions using only the provided repository context.
                - Do not invent files, classes, methods, APIs, dependencies, or behavior.
                - If the provided context is insufficient, clearly say that you do not have enough information.
                - Cite relevant source files when repository context supports the answer.
                - Be concise, accurate, and technical.
                - Distinguish between what the code currently does and suggestions for improvement.
                - Do not expose secrets, API keys, tokens, passwords, or other credentials.

                ## Repository

                Repository: %s
        """.formatted(repositoryFullName, repositoryFullName);
  }

  public String userPrompt(String codeContext, String question) {
    return """
        Use the following repository context to answer the user's question.

                ## Repository Context

                %s

                ## User Question

                %s

                Answer based only on the provided repository context.
        """.formatted(codeContext, question);
  }
}
