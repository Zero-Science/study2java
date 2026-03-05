package ai.chatgpt;

import org.apache.log4j.Logger;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputMessage.Content;

import ai.common.AiClient;
import ai.common.ApiConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class ChatgptAiClient implements AiClient {

	private static Logger logger = Logger.getLogger(ChatgptAiClient.class);

	private final ApiConfig config;

	public ChatgptAiClient(ApiConfig config) {
		this.config = config;
	}

	@Override
	public String call(String prompt) {

		OpenAIClient client = OpenAIOkHttpClient.builder()
				.apiKey(config.getApiKey())
				.build();

		ResponseCreateParams params = ResponseCreateParams.builder()
				.input(prompt)
				.model(config.getModel())
				.build();

		Response response = client.responses().create(params);

		String text = extractText(response);

		logger.info(text);

		return  text;

	}

	@Override
	public String call(ArrayList<HashMap<String, Object>> result,String prompt) {
		return "";
	}


	public static String extractText(Response response) {
		StringBuilder sb = new StringBuilder();

		for (ResponseOutputItem item : response.output()) {

			// 只处理 message 类型
			if (item.message().isPresent()) {
				ResponseOutputMessage message = item.message().get();

				for (Content content : message.content()) {
					if (content.isOutputText()){
						sb.append(content.outputText().get().text());
					}
				}
			}
		}

		return sb.toString();
	}
}
