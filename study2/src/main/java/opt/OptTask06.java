package opt;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ai.common.AiClient;
import ai.common.AiClientFactory;
import db.DBManager;
import util.HtmlUtil;

public class OptTask06 extends AiTaskExecutor {
	private Logger logger = Logger.getLogger(OptTask06.class);

	String api;
	String no;
	String div;

	private void init() {
		this.api = this.parameter[0];
		this.no = parameter[1];
		this.div = parameter[2];
	}

	protected String makePrompt() {

		ArrayList<Object> list = new ArrayList<Object>();
		StringBuffer query = new StringBuffer();

		query.append("SELECT  t.\"プロンプト詳細\" ");
		query.append("FROM  \"AI質問情報管理\" t ");
		query.append("WHERE t.\"番号\" = ? ");
		list.add(this.no);

		ArrayList<HashMap<String, Object>> result = DBManager.select(query.toString(), list);

		//    ArrayList<HashMap<String, Object>> result = DBManager.select(
		//            query.toString(),
		//            no,
		//            shopId
		//    );
		logger.info("result:" + result);

		String prompt = String.valueOf(result.get(0).get("プロンプト詳細"));
		if (prompt == null || prompt.isEmpty()) {
			logger.error("查询结果集为空，无法获取索引0的行数据！");
			throw new IllegalStateException("查询结果集为空，无法获取索引0的行数据！");

		}
		logger.info("word:" + prompt);

		return prompt;

	}

	protected String callAPI(String prompt) {
		AiClient client = AiClientFactory.getClient(this.api);
		this.logger.info("ai连接成功");
		return client.call(prompt);
	}

	protected Object analyzeResponse(String response) {

		logger.info("ai执行成功");
		String returnResponse = null;
		try {
			logger.info("解析响应内容: " + response);

			if ("文章".equals(div)) {
				returnResponse = response;
			} else if ("HTML".equals(div)) {
				returnResponse = HtmlUtil.toHtml(response);

			} else if ("JSON".equals(div)) {
				returnResponse = response;
			}

		} catch (Exception e) {
			logger.error("响应解析失败", e);
			throw new RuntimeException("响应解析失败", e);

		}
		return returnResponse;
	}

	protected void handleResult(Object parsedResult) throws Exception {

		logger.info("parsedResult:" + parsedResult);

		try {

			logger.info("新增开始");

			StringBuffer updateSQL = new StringBuffer();
			ArrayList<Object> list = new ArrayList<Object>();

			updateSQL.append("UPDATE \"AI質問情報管理\" ");
			updateSQL.append("SET ");
			updateSQL.append("\"回答\" = ?, ");
			list.add(parsedResult.toString());
			updateSQL.append("\"ステータス\" = '作成済', ");
			updateSQL.append("\"更新ID\" = 'batch', ");
			updateSQL.append("\"更新日時\" =  CURRENT_TIMESTAMP  ");

			updateSQL.append("WHERE ");
			updateSQL.append("\"番号\" = ? ");
			list.add(this.no);

			DBManager.update(updateSQL.toString(), list);

			logger.info("更新AI質問情報管理成功");
		} catch (Exception e) {
			// 5. 任意一次失败，手动回滚事务
			e.printStackTrace();
			logger.error("更新失败", e);
		}

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
