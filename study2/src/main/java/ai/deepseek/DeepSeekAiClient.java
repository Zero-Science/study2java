package ai.deepseek;

import ai.common.AiClient;
import ai.common.ApiConfig;
import ai.deepseek.response.ChatRequest;
import ai.deepseek.response.ChatResponse;
import ai.deepseek.response.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.*;
import org.apache.log4j.Logger;
import util.AIFileAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeepSeekAiClient implements AiClient {

	private static Logger logger = Logger.getLogger(DeepSeekAiClient.class);

	private final ApiConfig config;

	private final OkHttpClient client;
	private final ObjectMapper objectMapper;
	private AIFileAnalysis  aiFileAnalysis = new AIFileAnalysis();
	public DeepSeekAiClient(ApiConfig config) {
		this.config = config;
		this.client = new OkHttpClient.Builder()
				.connectTimeout(180, TimeUnit.SECONDS)
				.readTimeout(180, TimeUnit.SECONDS)
				.writeTimeout(180, TimeUnit.SECONDS)
				.build();
		this.objectMapper = new ObjectMapper();
	}

	//回答
	public String chatCompletion(String userMessage) throws IOException {
		List<Message> messages = new ArrayList<>();
		messages.add(new Message("user", userMessage));

		ChatRequest request = new ChatRequest(config.getModel(), messages);
		String requestBody = objectMapper.writeValueAsString(request);

		Request httpRequest = new Request.Builder()
				.url(config.getUrl())
				.post(RequestBody.create(requestBody, MediaType.parse("application/json")))
				.addHeader("Authorization", "Bearer " + config.getApiKey())
				.addHeader("Content-Type", "application/json")
				.build();

		try (Response response = client.newCall(httpRequest).execute()) {
			if (!response.isSuccessful()) {
				String errorBody = response.body() != null ? response.body().string() : "";
				throw new IOException("API请求失败: HTTP " + response.code() + " - " + errorBody);
			}

			String responseBody = response.body().string();

			// 调试：打印原始响应
			logger.info("原始响应: " + responseBody);

			ChatResponse chatResponse = objectMapper.readValue(responseBody, ChatResponse.class);

			if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
				Message message = chatResponse.getChoices().get(0).getMessage();
				if (message != null && message.getContent() != null) {
					return message.getContent();
				} else {
					return "响应内容为空";
				}
			} else {
				return "没有可用的响应选项";
			}
		}
	}

	@Override
	public String call(String prompt) {
		List<Message> messages = new ArrayList<>();
		messages.add(new Message("user", prompt));

		ChatRequest request = new ChatRequest(config.getModel(), messages);
		String requestBody = null;
		try {
			requestBody = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		Request httpRequest = new Request.Builder()
				.url(config.getUrl())
				.post(RequestBody.create(requestBody, MediaType.parse("application/json")))
				.addHeader("Authorization", "Bearer " + config.getApiKey())
				.addHeader("Content-Type", "application/json")
				.build();

		try (Response response = client.newCall(httpRequest).execute()) {
			if (!response.isSuccessful()) {
				String errorBody = response.body() != null ? response.body().string() : "";
				throw new IOException("API请求失败: HTTP " + response.code() + " - " + errorBody);
			}

			String responseBody = response.body().string();

			// 调试：打印原始响应
			logger.info("原始响应: " + responseBody);

			ChatResponse chatResponse = objectMapper.readValue(responseBody, ChatResponse.class);

			if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
				Message message = chatResponse.getChoices().get(0).getMessage();
				if (message != null && message.getContent() != null) {
					return message.getContent();
				} else {
					return "响应内容为空";
				}
			} else {
				return "没有可用的响应选项";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "错误";
	}

	@Override
	public String call(ArrayList<HashMap<String, Object>> result,String prompt) throws Exception {
		return "";
	}


}