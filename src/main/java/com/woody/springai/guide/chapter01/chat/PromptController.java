package com.woody.springai.guide.chapter01.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

@RestController
@RequestMapping("/prompt")
@RequiredArgsConstructor
public class PromptController {

    private final OpenAiChatModel openAiChatModel;

    @GetMapping("/ai/use-assistant")
    public Map<String,String> useAssistant(@RequestParam(value = "question") String question) {
        PromptTemplate promptTemplate = new PromptTemplate(question);
        promptTemplate.add("question", question);

        String render = promptTemplate.render();
        Message userMessage = new UserMessage(render);
        Message systemMessage = new SystemMessage(
                "당신은 항상 존댓말을 사용해야 합니다. " +
                        "그리고 상대를 '형님'이라고 부르며 공손하게 대해야 합니다. " +
                        "답변의 끝에는 항상 '행님!!'을 붙여야 합니다."
        );

        // 이전 AssistantMessage를 활용하는 로직 추가 (간단한 메모리 저장 방식)
        Message latestAssistantMessage = MessageHistory.getLatestAssistantMessage();

        String response;
        if (latestAssistantMessage != null) {
            response = openAiChatModel.call(systemMessage, latestAssistantMessage, userMessage);
        } else {
            response = openAiChatModel.call(systemMessage, userMessage);
        }

        // 새로운 AssistantMessage 저장
        MessageHistory.saveAssistantMessage(new AssistantMessage(response));
        return Map.of("response", response);
    }

    @GetMapping("/ai/not-used-assistant")
    public Map<String,String> notUsedAssistant(@RequestParam(value = "question") String question) {
        PromptTemplate promptTemplate = new PromptTemplate(question);
        promptTemplate.add("question", question);

        String render = promptTemplate.render();
        Message userMessage = new UserMessage(render);
        Message systemMessage = new SystemMessage(
                "당신은 항상 존댓말을 사용해야 합니다. " +
                        "그리고 상대를 '형님'이라고 부르며 공손하게 대해야 합니다. " +
                        "답변의 끝에는 항상 '행님!!'을 붙여야 합니다."
        );

        String response = openAiChatModel.call(userMessage, systemMessage);
        return Map.of("response", response);
    }

    class MessageHistory {

        private static final Deque<Message> MESSAGE_HISTORY = new ArrayDeque<>();

        // 마지막 AssistantMessage 가져오기
        public static Message getLatestAssistantMessage() {
            return MESSAGE_HISTORY.stream()
                    .filter(msg -> msg instanceof AssistantMessage)
                    .reduce((first, second) -> second) // 가장 최신 메시지 가져오기
                    .orElse(null);
        }

        // 새로운 AssistantMessage 저장
        public static void saveAssistantMessage(Message message) {
            if (message instanceof AssistantMessage) {
                MESSAGE_HISTORY.addLast(message);

                // 저장된 메시지가 너무 많아지면 오래된 것 삭제 (예: 10개 유지)
                if (MESSAGE_HISTORY.size() > 10) {
                    MESSAGE_HISTORY.removeFirst();
                }
            }
        }
    }
}