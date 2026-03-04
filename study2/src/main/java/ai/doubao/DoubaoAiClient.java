package ai.doubao;

import ai.common.AiClient;
import ai.common.ApiConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import org.apache.log4j.Logger;
import util.AIFileAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DoubaoAiClient implements AiClient {
	private static Logger log = Logger.getLogger(DoubaoAiClient.class);
	private final ApiConfig config;

	public DoubaoAiClient(ApiConfig config) {
		this.config = config;
	}
	AIFileAnalysis  aiFileAnalysis = new AIFileAnalysis();
	@Override
	public String call(String prompt) {

		ArkService arkService = ArkService.builder()
				.apiKey(config.getApiKey())
				.baseUrl(config.getUrl())
				.build();

		List<ChatMessage> chatMessages = new ArrayList<>();
		ChatMessage userMessage = ChatMessage.builder()
				.role(ChatMessageRole.USER)
				.content(prompt)
				.build();
		chatMessages.add(userMessage);

		// 创建聊天完成请求
		ChatCompletionRequest request = ChatCompletionRequest.builder()
				.model(config.getModel())
				.messages(chatMessages)
				.build();

		String content = (String) arkService.createChatCompletion(request)
				.getChoices()
				.get(0)
				.getMessage()
				.getContent();

		return content;
	}

//	文件解析
	/**
	 * 文件分析主方法
	 * @param result 数据库查询结果，每条记录包含 "拡張子", "元ファイル", "枝番号"（可选）
	 * @param prompt 提示语
	 * @return 模型返回的文本结果
	 * @throws Exception 处理异常
	 */
	public String fileAnalysis(ArrayList<HashMap<String, Object>> result, String prompt) throws Exception {
		// 第一步：校验扩展名
		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> record = result.get(i);
			Object extObj = record.get("拡張子");
			if (extObj == null) {
				throw new IllegalArgumentException("第" + (i + 1) + "条记录缺少拡張子");
			}
			String ext = extObj.toString().toLowerCase();
			// 使用 processFile 校验扩展名（返回 null 表示不支持）
			if (aiFileAnalysis.processFile("dummy." + ext) == null) {
				throw new IllegalArgumentException("不支持的文件扩展名: " + ext);
			}
		}

		// 第二步：将 Base64 转为文件，存储到指定目录，并收集绝对路径
		String baseDir = "C:\\Users\\sunny\\Desktop\\doc\\";
		File dir = new File(baseDir);
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("无法创建目录：" + baseDir);
		}

		List<String> localFilePaths = new ArrayList<>();
		for (int i = 0; i < result.size(); i++) {
			HashMap<String, Object> record = result.get(i);
//			后缀
			String ext = record.get("拡張子").toString().toLowerCase();
//			base64
			String base64Content = record.get("元ファイル").toString();
//			序号
			Object subNoObj = record.get("枝番号");
//			番号
			String no = record.get("番号").toString();
			String subNo = (subNoObj != null) ? subNoObj.toString() : String.valueOf(i + 1);
			String fileName = no + "-" +  subNo + "." + ext;
			String filePath = baseDir + fileName;

			aiFileAnalysis.decodeBase64ToFile(base64Content, filePath); // 将 Base64 解码为文件
			localFilePaths.add(filePath);
			log.info("已生成本地文件:"+ filePath);
		}
		// 第三步：根据后缀转换文档为 PDF，更新文件列表
		localFilePaths = AIFileAnalysis.convertDocumentsToPdf(localFilePaths);

		// 第四步：创建 ArkService，上传所有文件并等待处理完成
		ArkService service = null;
		try {
			service = ArkService.builder()
					.apiKey(config.getApiKey())
					.baseUrl(config.getUrl())
					.build();

			// 上传文件并获取 fileId 及类型信息
			List<AIFileAnalysis.UploadedFileInfo> uploadedFiles = aiFileAnalysis.uploadAllFiles(service, localFilePaths);

			// 第五步：根据文件类型构建请求
			CreateResponsesRequest request = aiFileAnalysis.buildMixedRequest(config,uploadedFiles, prompt);

			// 第六步：发送请求
			ResponseObject resp = service.createResponse(request);

			// 第七步：提取返回文本
			return aiFileAnalysis.extractResponseText(resp);

		} finally {
			// 第八步：关闭 service
			if (service != null) {
				service.shutdownExecutor();
			}
		}
	}



}
