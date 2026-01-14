package ai.deepseek.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Usage 类
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage {
	@JsonProperty("prompt_tokens")
	private int promptTokens;

	@JsonProperty("completion_tokens")
	private int completionTokens;

	@JsonProperty("total_tokens")
	private int totalTokens;

	// getters and setters
	public int getPromptTokens() {
		return promptTokens;
	}

	public void setPromptTokens(int promptTokens) {
		this.promptTokens = promptTokens;
	}

	public int getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(int completionTokens) {
		this.completionTokens = completionTokens;
	}

	public int getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(int totalTokens) {
		this.totalTokens = totalTokens;
	}
}
