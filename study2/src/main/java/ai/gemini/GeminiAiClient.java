package ai.gemini;

import org.apache.log4j.Logger;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import ai.common.AiClient;
import ai.common.ApiConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class GeminiAiClient implements AiClient {

	private static Logger logger = Logger.getLogger(GeminiAiClient.class);

	private final ApiConfig config;

	public GeminiAiClient(ApiConfig config) {
		this.config = config;
	}

	@Override
	public String call(ArrayList<HashMap<String, Object>> result, String prompt) throws Exception {
		return "";
	}

	@Override
	public String call(String prompt) {

		Client client = Client.builder()
				.apiKey(this.config.getApiKey())
				.build();


		logger.info("Model:" + config.getModel());
		logger.info("Prompt:" + prompt);
		try {
			// 2. 调用模型生成内容
			// 模型名推荐使用 "gemini-1.5-flash" (快且便宜) 或 "gemini-1.5-pro" (更强大)
			GenerateContentResponse response = client.models.generateContent(
					this.config.getModel(),
					prompt,
					null);

			// 3. 输出结果
			logger.info("AI 回复: " + response.text());

			return response.text();

		} catch (Exception e) {
			logger.error("调用失败: " + e.getMessage());
			e.printStackTrace();
		}
		return "";

	}


}
