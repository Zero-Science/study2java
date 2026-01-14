package opt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

public class OptVoiceEN {
	Logger logger = Logger.getLogger(OptVoiceEN.class);

	public void excute(String[] parameter) throws Exception {
		logger.info("==================OptEnglishVoice.excute実行開始！==================");
		String api = parameter[0];
		String mp3folder = parameter[1];
		String book = parameter[2];
		String classification = parameter[3];
		String wordseq = parameter[4];
		String content = parameter[5];
		String div = parameter[6];
		String type = parameter[7];
		if ("youdaoAPI".equals(api)) {
			downloadVoiceFromYoudao(mp3folder, book, classification, wordseq, content, div, type);
		} else if ("googleAPI".equals(api)) {
			downloadVoiceFromGoogleAPI(mp3folder, book, classification, wordseq, content, div, type);
		}
		logger.info("==================OptEnglishVoice.excute実行終了！==================");
	}

	private void downloadVoiceFromYoudao(String mp3folder, String book, String classification, String wordseq,
			String content, String div, String type) throws IOException {
		String filePath = mp3folder + "//" + book + "//" + classification + "//";
		String fileName = wordseq + "-" + div + "-" + type + ".mp3";

		try {
			File folder = new File(filePath);

			if (!folder.exists()) {
				folder.mkdirs();
			}

			File file = new File(filePath + fileName);
			if (!file.exists()) {
				URL url = new URL("http://dict.youdao.com/dictvoice?type=" + type + "&audio=" + content);

				try (InputStream in = url.openStream();
						OutputStream out = Files.newOutputStream(Paths.get(filePath + fileName))) {

					byte[] buffer = new byte[4096];
					int bytesRead;

					while ((bytesRead = in.read(buffer)) != -1) {
						out.write(buffer, 0, bytesRead);
					}

					System.out.println("MP3文件下载完成: " + filePath + fileName);
				}
			}

		} catch (Exception e) {
			logger.error("音声取得失敗しました。[" + book + "][" + classification + "][" + wordseq + "]【" + content + "】[" + div
					+ "][" + type + "]");
			logger.error("音声取得失敗しました。", e);
			e.printStackTrace();

		}
	}

	private void downloadVoiceFromGoogleAPI(String mp3folder, String book, String classification, String wordseq,
			String content, String div, String type) throws IOException {
		//      String word = "method";  // 指定英语单词
		//      String output = "method.mp3";
		String filePath = mp3folder + "//" + book + "//" + classification + "//";
		String fileName = wordseq + "-" + div + "-" + type + ".mp3";

		// 构建输入（文本）
		SynthesisInput input = SynthesisInput.newBuilder()
				.setText(content)
				.build();

		// 声音设置（英语，默认女声）
		VoiceSelectionParams voice = null;
		// アメリカ
		if ("0".equals(type)) {
			voice = VoiceSelectionParams.newBuilder()
					.setLanguageCode("en-US")
					.setName("en-US-Wavenet-D") // 可改为其它声音
					.build();
		} else if ("1".equals(type)) {
			voice = VoiceSelectionParams.newBuilder()
					.setLanguageCode("en-GB")
					.setName("en-GB-Wavenet-A") // 英音男声
					.build();
		}

		// 输出格式设置（MP3）
		AudioConfig audioConfig = AudioConfig.newBuilder()
				.setAudioEncoding(AudioEncoding.MP3)
				.build();

		// 调用 API
		try (TextToSpeechClient tts = TextToSpeechClient.create()) {

			byte[] audioContents = tts.synthesizeSpeech(input, voice, audioConfig)
					.getAudioContent()
					.toByteArray();

			// 保存 MP3 文件
			try (OutputStream out = new FileOutputStream(filePath + fileName)) {
				out.write(audioContents);
				logger.info("Saved: " + filePath + fileName);
			}
		}
	}
}
