package util;

import ai.common.ApiConfig;
import com.volcengine.ark.runtime.model.files.FileMeta;
import com.volcengine.ark.runtime.model.files.PreprocessConfigs;
import com.volcengine.ark.runtime.model.files.UploadFileRequest;
import com.volcengine.ark.runtime.model.files.Video;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.*;
import com.volcengine.ark.runtime.model.responses.item.BaseItem;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AIFileAnalysis {
    private static Logger log = Logger.getLogger(AIFileAnalysis.class);

    /**
     * 等待文件处理完成
     *
     * @param service ArkService 实例
     * @param fileId  文件ID
     * @return 处理完成的 FileMeta 对象，如果超时或失败返回 null
     */
    public  FileMeta waitForFileProcessing(ArkService service, String fileId) {
        int maxAttempts = 30;      // 最大尝试次数（30次 * 2秒 = 60秒）
        int intervalSeconds = 2;   // 每次检查间隔

        for (int i = 0; i < maxAttempts; i++) {
            try {
                FileMeta fileMeta = service.retrieveFile(fileId);
                String status = fileMeta.getStatus();
                log.info("尝试:"+i + 1 +"/"+maxAttempts+"次，"+"文件状态:"+status);
                // 成功状态
                if ("processed".equals(status) || "succeeded".equals(status) || "active".equals(status)) {
                    return fileMeta;
                }
                // 失败状态
                if ("failed".equals(status) || "error".equals(status)) {
                    log.error("文件处理失败，状态为: " + status);

                    return null;
                }

                TimeUnit.SECONDS.sleep(intervalSeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待被中断");
                return null;
            } catch (Exception e) {
                log.error("获取文件状态时出错: " + e.getMessage());
                return null;
            }
        }
        log.error("等待文件处理超时 " + (maxAttempts * intervalSeconds) + " 秒");
        return null;
    }


    /**
     * 获取文件后缀（不包含点）
     *
     * @param filePath 文件的绝对路径
     * @return 文件后缀（如 "txt", "jpg"），如果没有后缀则返回空字符串
     */
    public  String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        // 将路径转换为File对象，以正确处理不同操作系统的路径分隔符
        File file = new File(filePath);
        String fileName = file.getName();

        // 查找最后一个点号的位置
        int dotIndex = fileName.lastIndexOf('.');
        // 如果点号存在且不在首位（即不是隐藏文件且确实有扩展名）
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    //文件后缀转化
    public  String processFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        String extension = getFileExtension(filePath).toLowerCase();
        if (extension.isEmpty()) {
            // 没有扩展名，可根据需求处理
            return null;
        }

        // 图片格式
        if (extension.matches("jpg|jpeg|png|gif|webp|bmp|tiff|ico|icns|sgi|jp2|heic|heif")) {
            return "image";
        }
        // 视频格式
        if (extension.matches("mp4|avi|mov")) {
            return "video";
        }
        // PDF
        if (extension.equals("pdf")) {
            return "pdf";
        }
        // Word文档和文本文件
        if (extension.equals("docx") || extension.equals("txt")) {
            // 调用batchConvertFiles方法转换成PDF
            return "pdf";
        }
        // Excel文件
        if (extension.equals("xls") || extension.equals("xlsx") ) {
            // 注意：.excel可能指代Excel文件，实际扩展名是xls/xlsx
            return "pdf";
        }
        // 其他格式，可以返回null或抛出异常
        log.error("当前文件格式不匹配");
        return null;
    }


    /**
     * 上传所有文件并等待处理完成，返回每个文件的 fileId 及类型
     */
    /**
     * 上传所有文件并等待处理完成（兼容低版本 SDK，不使用 builder）
     */
    public  List<UploadedFileInfo> uploadAllFiles(ArkService service, List<String> localFilePaths) throws Exception {
        List<UploadedFileInfo> uploadedFiles = new ArrayList<>();

        for (String filePath : localFilePaths) {
            File file = new File(filePath);
            String fileName = file.getName();
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            String fileType = processFile("dummy." + ext);

            // 创建 UploadFileRequest 对象（假设有无参构造）
            UploadFileRequest request = new UploadFileRequest();
            request.setFile(file);
            request.setPurpose("user_data");

            if ("video".equals(fileType)) {
                // 创建预处理配置
                PreprocessConfigs preprocess = new PreprocessConfigs();
                Video video = new Video(0.3); // 抽帧间隔 0.3 秒
                preprocess.setVideo(video);
                request.setPreprocessConfigs(preprocess);
            }

            FileMeta fileMeta = service.uploadFile(request);
            log.info("已上传文件: "+fileName+",ID: "+fileMeta.getId()+", 类型: "+fileType );
            uploadedFiles.add(new UploadedFileInfo(fileMeta.getId(), fileType));
        }

        // 等待所有文件处理完成
        for (UploadedFileInfo info : uploadedFiles) {
            waitForFileProcessing(service, info.fileId);
        }

        return uploadedFiles;
    }

    /**
     * 上传文件信息
     */
    public  class UploadedFileInfo {
        String fileId;
        String fileType; // "image", "video", "document"

        UploadedFileInfo(String fileId, String fileType) {
            this.fileId = fileId;
            this.fileType = fileType;
        }
    }


    /**
     * 从 TXT 文件中读取 Base64 内容，解码并保存为指定文件（兼容 Java 8）
     * @param txtFilePath  包含 Base64 字符串的文本文件路径
     * @param outputPath   输出文件的完整路径（需包含扩展名，如 "C:/doc/1.jpg"）
     * @throws IOException 文件读取/写入失败或 Base64 解码失败时抛出
     */
    public  void decodeBase64ToFile(String txtFilePath, String outputPath) throws IOException {
        // 1. 获取base64参数
        String content =txtFilePath;

        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("获取源文件数据为空");
        }

        // 2. 去除空白字符（换行、空格等）
        String cleanBase64 = content.replaceAll("\\s", "");

        // 3. 去除可能的 data URL 前缀（如 "data:image/png;base64,"）
        if (cleanBase64.contains(",")) {
            cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
        }

        // 4. Base64 解码
        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new IOException("Base64 解码失败，请检查文件内容格式", e);
        }

        // 5. 确保父目录存在并写入文件
        java.nio.file.Path output = Paths.get(outputPath);
        Files.createDirectories(output.getParent());
        Files.write(output, decodedBytes);
    }


    /**
     * 构建混合类型请求
     */
    public CreateResponsesRequest buildMixedRequest(ApiConfig config,List<UploadedFileInfo> uploadedFiles, String prompt) {
        MessageContent.Builder contentBuilder = MessageContent.builder();

        for (UploadedFileInfo info : uploadedFiles) {
            switch (info.fileType) {
                case "image":
                    contentBuilder.addListItem(InputContentItemImage.builder()
                            .fileId(info.fileId)
                            .build());
                    break;
                case "video":
                    contentBuilder.addListItem(InputContentItemVideo.builder()
                            .fileId(info.fileId)
                            .build());
                    break;
                case "pdf":
                case "document":
                default:
                    contentBuilder.addListItem(InputContentItemFile.InputContentItemFileBuilder
                            .anInputContentItemFile()
                            .fileId(info.fileId)
                            .build());
                    break;
            }
        }

        contentBuilder.addListItem(InputContentItemText.builder()
                .text(prompt)
                .build());

        return CreateResponsesRequest.builder()
                .model(config.getModel())
                .input(ResponsesInput.builder()
                        .addListItem(ItemEasyMessage.builder()
                                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                .content(contentBuilder.build())
                                .build())
                        .build())
                .build();
    }

    /**
     * 提取模型返回的文本内容
     */
    public String extractResponseText(ResponseObject resp) {
        StringBuilder result = new StringBuilder();
        List<BaseItem> outputList = resp.getOutput();
        if (outputList != null) {
            for (BaseItem item : outputList) {
                if (item instanceof ItemOutputMessage) {
                    ItemOutputMessage message = (ItemOutputMessage) item;
                    List<OutputContentItem> contentItems = message.getContent();
                    if (contentItems != null) {
                        for (OutputContentItem contentItem : contentItems) {
                            if (contentItem instanceof OutputContentItemText) {
                                OutputContentItemText textItem = (OutputContentItemText) contentItem;
                                String text = textItem.getText();
                                if (text != null && !text.trim().isEmpty()) {
                                    result.append(text).append("\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        return result.toString().trim();
    }


    /**
     * 将文档类文件（docx, txt, xls, xlsx）转换为 PDF，并替换文件列表中的路径
     */
    public static List<String> convertDocumentsToPdf(List<String> filePaths) throws Exception {
        List<String> convertedPaths = new ArrayList<>();
        for (String path : filePaths) {
            String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            if (ext.equals("docx") || ext.equals("txt")) {
                String pdfPath = path.substring(0, path.lastIndexOf('.')) + ".pdf";
                ConvertFile.convertFileToPdf(path, pdfPath);
                convertedPaths.add(pdfPath);
                // 可选：删除原文件
                new File(path).delete();
            } else if (ext.equals("xls") || ext.equals("xlsx")) {
                String pdfPath = path.substring(0, path.lastIndexOf('.')) + ".pdf";
                ConvertFile.convertExcelToPdf(path, pdfPath);
                convertedPaths.add(pdfPath);
                new File(path).delete();
            } else {
                convertedPaths.add(path); // 非文档文件保持原样
            }
        }
        return convertedPaths;
    }
}
