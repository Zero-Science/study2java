package ai.common;

import java.util.HashMap;
import java.util.Map;

public class ApiConfig {
	private String ai;
	private String model;
	private String apiKey;
	private String url;
	private Map<String, Object> extra = new HashMap<>();

	public ApiConfig() {
	}

	public ApiConfig(String ai, String model, String apiKey, String url) {
		this.ai = ai;
		this.model = model;
		this.apiKey = apiKey;
		this.url = url;
	}

	public String getAi() {
		return ai;
	}

	public String getModel() {
		return model;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getUrl() {
		return url;
	}

	public Map<String, Object> getExtra() {
		return extra;
	}

	public ApiConfig withExtra(String key, Object value) {
		this.extra.put(key, value);
		return this;
	}
}