package ai.chatgpt;

import org.apache.log4j.Logger;

import ai.common.AiClient;
import ai.common.ApiConfig;

public class ChatgptAiClient implements AiClient {

	private static Logger logger = Logger.getLogger(ChatgptAiClient.class);

	private final ApiConfig config;

	public ChatgptAiClient(ApiConfig config) {
		this.config = config;
	}

	@Override
	public String call(String prompt) {

		return null;

	}
}
