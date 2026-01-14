package ai.deepseek.response;

import java.util.List;

public class ChatRequest {
	private String model;
	private List<Message> messages;
	private double temperature;
	private int maxTokens;

	public ChatRequest(String model, List<Message> messages) {
		this.model = model;
		this.messages = messages;
		this.temperature = 0.7;
		this.maxTokens = 2048;
	}

	// getters and setters
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public int getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
	}
}
