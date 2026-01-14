package ai.deepseek.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Choice 类
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {
	private int index;
	private Message message;
	private String finishReason;

	@JsonProperty("finish_reason")
	public String getFinishReason() {
		return finishReason;
	}

	@JsonProperty("finish_reason")
	public void setFinishReason(String finishReason) {
		this.finishReason = finishReason;
	}

	// getters and setters
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
}