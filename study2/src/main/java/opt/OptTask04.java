package opt;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ai.common.AiClient;
import ai.common.AiClientFactory;
import db.DBManager;
import util.HtmlUtil;

public class OptTask04 extends AiTaskExecutor {
	private Logger logger = Logger.getLogger(OptTask04.class);

	String api;
	String book;
	String classification;
	String wordseq;
	String word;
	String flg;

	private void init() {
		this.api = this.parameter[0];
		this.book = this.parameter[1];
		this.classification = this.parameter[2];
		this.wordseq = this.parameter[3];
		this.word = this.parameter[4];
		this.flg = this.parameter[5];
	}

	protected String makePrompt() {
		String prompt = "你是日语老师，请讲解一下[" + this.word + "]这个日语单词。";
		return prompt;
	}

	protected String callAPI(String prompt) {
		AiClient client = AiClientFactory.getClient(this.parameter[0]);
		this.logger.info("ai连接成功");
		return client.call(prompt);
	}

	protected Object analyzeResponse(String rawResponse) {
		this.logger.info("ai执行成功");
		try {
			this.logger.info("解析响应内容: " + rawResponse);
			return HtmlUtil.toHtml(rawResponse);
		} catch (Exception e) {
			this.logger.error("响应解析失败", e);
			throw new RuntimeException("响应解析失败", e);
		}
	}

	protected void handleResult(Object parsedResult) {
		String html = (String) parsedResult;
		StringBuffer updateSQL = new StringBuffer();
		ArrayList<Object> list = new ArrayList<Object>();
		updateSQL.append("UPDATE \"STY_単語情報\" SET ");
		if ("ai1ch".equals(this.flg)) {
			updateSQL.append("\"AI説明_AI1_中国語\" = ? ");
			list.add(html);
		} else if ("ai2ch".equals(this.flg)) {
			updateSQL.append("\"AI説明_AI2_中国語\" = ? ");
			list.add(html);
		}
		updateSQL.append(", \"更新ID\" = ? ");
		list.add("batch");
		updateSQL.append(", \"更新日時\" = CURRENT_TIMESTAMP ");
		updateSQL.append("WHERE ");
		updateSQL.append("\"書籍\" = ? AND");
		list.add(this.book);
		updateSQL.append("\"分類\" = ? AND");
		list.add(this.classification);
		updateSQL.append("\"単語SEQ\" = ? ");
		list.add(Integer.parseInt(this.wordseq));
		this.logger.info(updateSQL.toString());
		DBManager.update(updateSQL.toString(), list);
	}

	protected void finish() {
		this.logger.info("流程结束");
	}

	public void excute(String[] parameter) throws Exception {
		setParameter(parameter);
		init();
		process();
	}
}
