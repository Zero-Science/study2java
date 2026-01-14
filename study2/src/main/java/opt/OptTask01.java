package opt;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ai.common.AiClient;
import ai.common.AiClientFactory;
import db.DBManager;

public class OptTask01 extends AiTaskExecutor {
	private Logger logger = Logger.getLogger(OptTask01.class);

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
		String language = "中";
		if ("jp".equals(this.flg))
			language = "日";
		String prompt = "请用[" + this.word + "]这个词出一个英译" + language + "的六选一的选择题，中间有一个正确答案和五个混淆项。\n" +
				"题目是一句带有[" + this.word + "]的句子，要求选出这句话中[" + this.word + "]的" + language + "文含义。\n" +
				"请按下面格式输出，不需要其他多余文字。\n" +
				"【题目】: XXXXX\n" +
				"【正确翻译】: XXXXX\n" +
				"【混淆干扰项1】: XXXXX\n" +
				"【混淆干扰项2】: XXXXX\n" +
				"【混淆干扰项3】: XXXXX\n" +
				"【混淆干扰项4】: XXXXX\n" +
				"【混淆干扰项5】: XXXXX\n" +
				"【讲解内容】: XXXXX";
		return prompt;
	}

	protected String callAPI(String prompt) {
		AiClient client = AiClientFactory.getClient(this.parameter[0]);
		this.logger.info("ai连接成功");
		return client.call(prompt);
	}

	protected Object analyzeResponse(String response) {
		HashMap<String, String> returnValue = new HashMap<>();
		String[] responseArray = response.split("\n");
		String div = "英訳中";
		if ("jp".equals(this.flg))
			div = "英訳日";
		returnValue.put("div", div);
		String title = null;
		String correctItem = null;
		String wrongItem1 = null;
		String wrongItem2 = null;
		String wrongItem3 = null;
		String wrongItem4 = null;
		String wrongItem5 = null;
		String describe = null;
		for (int i = 0; i < responseArray.length; i++) {
			String line = responseArray[i];
			if (line.startsWith("【题目】: ")) {
				title = line.replaceAll("【题目】: ", "");
				returnValue.put("title", title);
			} else if (line.startsWith("【正确翻译】: ")) {
				correctItem = line.replaceAll("【正确翻译】: ", "");
				returnValue.put("correctItem", correctItem);
			} else if (line.startsWith("【混淆干扰项1】: ")) {
				wrongItem1 = line.replaceAll("【混淆干扰项1】: ", "");
				returnValue.put("wrongItem1", wrongItem1);
			} else if (line.startsWith("【混淆干扰项2】: ")) {
				wrongItem2 = line.replaceAll("【混淆干扰项2】: ", "");
				returnValue.put("wrongItem2", wrongItem2);
			} else if (line.startsWith("【混淆干扰项3】: ")) {
				wrongItem3 = line.replaceAll("【混淆干扰项3】: ", "");
				returnValue.put("wrongItem3", wrongItem3);
			} else if (line.startsWith("【混淆干扰项4】: ")) {
				wrongItem4 = line.replaceAll("【混淆干扰项4】: ", "");
				returnValue.put("wrongItem4", wrongItem4);
			} else if (line.startsWith("【混淆干扰项5】: ")) {
				wrongItem5 = line.replaceAll("【混淆干扰项5】: ", "");
				returnValue.put("wrongItem5", wrongItem5);
			} else if (line.startsWith("【讲解内容】: ")) {
				describe = line.replaceAll("【讲解内容】: ", "");
				returnValue.put("describe", describe);
			}
		}
		return returnValue;
	}

	@SuppressWarnings("unchecked")
	protected void handleResult(Object parsedResult) throws Exception {
		HashMap<String, String> returnValue = (HashMap<String, String>) parsedResult;
		StringBuffer insertSQL = new StringBuffer();
		ArrayList<Object> list = new ArrayList<Object>();
		insertSQL.append("insert into \"STY_単語質問情報\" ( ");
		insertSQL.append(" \"書籍\" ,");
		insertSQL.append("\"分類\" ,");
		insertSQL.append("\"単語SEQ\" ,");
		insertSQL.append("\"連番\" ,");
		insertSQL.append("\"区分1\" ,");
		insertSQL.append("\"区分2\" ,");
		insertSQL.append("\"質問\"  ,");
		insertSQL.append("\"正解\"  ,");
		insertSQL.append("\"誤解1\"  ,");
		insertSQL.append("\"誤解2\"  ,");
		insertSQL.append("\"誤解3\"  ,");
		insertSQL.append("\"誤解4\"  ,");
		insertSQL.append("\"誤解5\"  ,");
		insertSQL.append("\"説明\"  ,");
		insertSQL.append("\"登録ID\"  ,");
		insertSQL.append("\"更新ID\"  ,");
		insertSQL.append("\"登録日時\"  ,");
		insertSQL.append("\"更新日時\"  ");
		insertSQL.append(") values ( ");
		insertSQL.append("  ?,  ");
		list.add(this.book);
		insertSQL.append("  ?,  ");
		list.add(this.classification);
		insertSQL.append("  ?,  ");
		list.add(Integer.valueOf(Integer.parseInt(this.wordseq)));
		insertSQL.append("  0,  ");
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("div"));
		if ("jp".equals(this.flg)) {
			insertSQL.append("  ?,  ");
			list.add("日本語");
		} else {
			insertSQL.append("  ?,  ");
			list.add("中国語");
		}
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("title"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("correctItem"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("wrongItem1"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("wrongItem2"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("wrongItem3"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("wrongItem4"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("wrongItem5"));
		insertSQL.append("  ?,  ");
		list.add(returnValue.get("describe"));
		insertSQL.append("  ?,  ");
		list.add("batch");
		insertSQL.append("  ?,  ");
		list.add("batch");
		insertSQL.append("  CURRENT_TIMESTAMP,  ");
		insertSQL.append("  CURRENT_TIMESTAMP  ");
		insertSQL.append(")");
		DBManager.update(insertSQL.toString(), list);
	}

	protected void finish() {
		logger.info("流程结束");
	}

	public void excute(String[] parameter) throws Exception {
		logger.info("parameter:" + parameter.toString());
		setParameter(parameter);
		init();
		process();
	}
}
