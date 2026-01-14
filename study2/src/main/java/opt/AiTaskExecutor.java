package opt;

import org.apache.log4j.Logger;

public abstract class AiTaskExecutor {
	protected Logger logger = Logger.getLogger(getClass());

	protected String[] parameter;

	public void setParameter(String[] parameter) {
		this.parameter = parameter;
	}

	public final void process() throws Exception {
		this.logger.info("================== " + getClass().getSimpleName() + " 执行开始！==================");
		try {
			String prompt = makePrompt();
			this.logger.info("生成提示词：" + prompt);
			String rawResponse = callAPI(prompt);
			this.logger.info("API返回原始内容：" + rawResponse);
			Object parsedResult = analyzeResponse(rawResponse);
			this.logger.info("响应解析完成");
			handleResult(parsedResult);
			this.logger.info("结果处理完成");
			finish();
			this.logger.info("任务结束处理完成");
		} catch (Exception e) {
			this.logger.error("任务执行异常", e);
			throw e;
		} finally {
			this.logger.info("================== " + getClass().getSimpleName() + " 执行结束！==================");
		}
	}

	protected abstract String makePrompt();

	protected abstract String callAPI(String paramString) throws Exception;

	protected abstract Object analyzeResponse(String paramString) throws Exception;

	protected abstract void handleResult(Object paramObject) throws Exception;

	protected abstract void finish() throws Exception;
}
