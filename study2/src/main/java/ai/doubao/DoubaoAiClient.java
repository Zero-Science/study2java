package ai.doubao;

import java.util.ArrayList;
import java.util.List;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;

import ai.common.AiClient;
import ai.common.ApiConfig;

public class DoubaoAiClient implements AiClient {

	private final ApiConfig config;

	public DoubaoAiClient(ApiConfig config) {
		this.config = config;
	}

	@Override
	public String call(String prompt) {

		ArkService arkService = ArkService.builder()
				.apiKey(config.getApiKey())
				.baseUrl(config.getUrl())
				.build();

		List<ChatMessage> chatMessages = new ArrayList<>();
		ChatMessage userMessage = ChatMessage.builder()
				.role(ChatMessageRole.USER)
				.content(prompt)
				.build();
		chatMessages.add(userMessage);

		// 创建聊天完成请求
		ChatCompletionRequest request = ChatCompletionRequest.builder()
				.model(config.getModel())
				.messages(chatMessages)
				.build();

		String content = (String) arkService.createChatCompletion(request)
				.getChoices()
				.get(0)
				.getMessage()
				.getContent();

		return content;
	}
}
