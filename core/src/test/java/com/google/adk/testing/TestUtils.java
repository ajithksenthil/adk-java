/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.adk.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.InvocationContext;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.artifacts.InMemoryArtifactService;
import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.LlmResponse;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Utility methods for testing. */
public final class TestUtils {

  public static InvocationContext createInvocationContext(BaseAgent agent, RunConfig runConfig) {
    InMemorySessionService sessionService = new InMemorySessionService();
    return InvocationContext.create(
        sessionService,
        new InMemoryArtifactService(),
        "invocationId",
        agent,
        sessionService.createSession("test-app", "test-user").blockingGet(),
        Content.fromParts(Part.fromText("user content")),
        runConfig);
  }

  public static InvocationContext createInvocationContext(BaseAgent agent) {
    return createInvocationContext(agent, RunConfig.builder().build());
  }

  public static Event createEvent(String id) {
    return Event.builder()
        .id(id)
        .invocationId("invocationId")
        .author("author")
        .content(Content.fromParts(Part.fromText("content for event " + id)))
        .build();
  }

  public static Event createEscalateEvent(String id) {
    return createEvent(id).toBuilder()
        .actions(EventActions.builder().escalate(true).build())
        .build();
  }

  public static ImmutableList<Object> simplifyEvents(List<Event> events) {
    return events.stream()
        .map(
            event -> {
              if (event.content().isPresent() && event.content().get().parts().isPresent()) {
                List<Part> parts = event.content().get().parts().get();
                if (parts.size() == 1) {
                  Part part = parts.get(0);
                  if (part.text().isPresent()) {
                    return event.author() + ": " + part.text().get();
                  } else if (part.functionCall().isPresent()) {
                    // Custom formatting for FunctionCall
                    FunctionCall fc = part.functionCall().get();
                    String argsString =
                        fc.args().map(Object::toString).orElse("{}"); // Handle optional args
                    return String.format(
                        "%s: FunctionCall(name=%s, args=%s)",
                        event.author(), fc.name().orElse(""), argsString);
                  } else if (part.functionResponse().isPresent()) {
                    // Custom formatting for FunctionResponse
                    FunctionResponse fr = part.functionResponse().get();
                    String responseString =
                        fr.response()
                            .map(Object::toString)
                            .orElse("{}"); // Handle optional response
                    return String.format(
                        "%s: FunctionResponse(name=%s, response=%s)",
                        event.author(), fr.name().orElse(""), responseString);
                  }
                } else { // Multiple parts, return the list of parts for simplicity
                  // Apply custom formatting to parts within the list if needed
                  String partsString =
                      parts.stream()
                          .map(
                              part -> {
                                if (part.text().isPresent()) {
                                  return part.text().get();
                                } else if (part.functionCall().isPresent()) {
                                  FunctionCall fc = part.functionCall().get();
                                  String argsString = fc.args().map(Object::toString).orElse("{}");
                                  return String.format(
                                      "FunctionCall(name=%s, args=%s)",
                                      fc.name().orElse(""), argsString);
                                } else if (part.functionResponse().isPresent()) {
                                  FunctionResponse fr = part.functionResponse().get();
                                  String responseString =
                                      fr.response().map(Object::toString).orElse("{}");
                                  return String.format(
                                      "FunctionResponse(name=%s, response=%s)",
                                      fr.name().orElse(""), responseString);
                                }
                                return part.toString(); // Fallback
                              })
                          .collect(joining(", "));
                  return event.author() + ": [" + partsString + "]";
                }
              }
              return event.author() + ": [NO_CONTENT]"; // Fallback if no content/parts
            })
        .collect(toImmutableList());
  }

  public static void assertEqualIgnoringFunctionIds(
      Content actualContent, Content expectedContent) {
    assertThat(overwriteFunctionIdsInContent(actualContent))
        .isEqualTo(overwriteFunctionIdsInContent(expectedContent));
  }

  private static Content overwriteFunctionIdsInContent(Content content) {
    if (content.parts().isEmpty()) {
      return content;
    }
    return content.toBuilder()
        .parts(
            content.parts().get().stream()
                .map(TestUtils::overwriteFunctionIdsInPart)
                .collect(toImmutableList()))
        .build();
  }

  private static Part overwriteFunctionIdsInPart(Part part) {
    if (part.functionCall().isPresent()) {
      return part.toBuilder()
          .functionCall(
              part.functionCall().get().toBuilder().id("<overwritten by TestUtils>").build())
          .build();
    }
    if (part.functionResponse().isPresent()) {
      return part.toBuilder()
          .functionResponse(
              part.functionResponse().get().toBuilder().id("<overwritten by TestUtils>").build())
          .build();
    }
    return part;
  }

  public static TestBaseAgent createRootAgent(BaseAgent... subAgents) {
    return createRootAgent(Arrays.asList(subAgents));
  }

  public static TestBaseAgent createRootAgent(List<? extends BaseAgent> subAgents) {
    return new TestBaseAgent("root", /* eventSupplier= */ Flowable::empty, subAgents);
  }

  public static TestBaseAgent createSubAgent(String name) {
    return createSubAgent(name, Flowable::empty);
  }

  public static TestBaseAgent createSubAgent(String name, Event... events) {
    return createSubAgent(name, Flowable.fromArray(events));
  }

  public static TestBaseAgent createSubAgent(String name, Flowable<Event>... eventSeries) {
    return createSubAgent(name, Arrays.asList(eventSeries).iterator()::next);
  }

  public static TestBaseAgent createSubAgent(String name, Supplier<Flowable<Event>> eventSupplier) {
    return new TestBaseAgent(name, eventSupplier, /* subAgents= */ ImmutableList.of());
  }

  // TODO: b/414071046 Deprecate.
  public static LlmAgent createTestAgent(BaseLlm llm) {
    return createTestAgentBuilder(llm).build();
  }

  // TODO: b/414071046 Make this return TestAgent. It can be used with toBuilder().
  public static LlmAgent.Builder createTestAgentBuilder(BaseLlm llm) {
    return LlmAgent.builder().name("test agent").description("test agent description").model(llm);
  }

  public static TestLlm createTestLlm(LlmResponse response) {
    return createTestLlm(() -> Flowable.just(response));
  }

  public static TestLlm createTestLlm(Flowable<LlmResponse>... responses) {
    return createTestLlm(Arrays.asList(responses).iterator()::next);
  }

  public static TestLlm createTestLlm(LlmResponse... responses) {
    return new TestLlm(Arrays.asList(responses));
  }

  public static TestLlm createTestLlm(Supplier<Flowable<LlmResponse>> responsesSupplier) {
    return new TestLlm(responsesSupplier);
  }

  public static LlmResponse createLlmResponse(Content content) {
    return LlmResponse.builder().content(content).build();
  }

  public static class EchoTool extends BaseTool {
    public EchoTool() {
      super("echo_tool", "description");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
      return Optional.of(FunctionDeclaration.builder().name("echo_tool").build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
      return Single.just(ImmutableMap.<String, Object>builder().put("result", args).buildOrThrow());
    }
  }

  public static class FailingEchoTool extends BaseTool {
    public FailingEchoTool() {
      super("echo_tool", "description");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
      return Optional.of(FunctionDeclaration.builder().name("echo_tool").build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
      return Single.error(new RuntimeException("error"));
    }
  }

  private TestUtils() {}
}
