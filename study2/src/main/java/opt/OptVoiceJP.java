package opt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

public class OptVoiceJP {
	Logger logger = Logger.getLogger(OptVoiceJP.class);

	public void excute(String[] parameter) throws Exception {
		logger.info("==================OptEnglishVoice.excute実行開始！==================");
		String api = parameter[0];
		String mp3folder = parameter[1];
		String book = parameter[2];
		String classification = parameter[3];
		String wordseq = parameter[4];
		String content = parameter[5];
		String div = parameter[6];

		if ("googleAPI".equals(api)) {
			downloadVoiceFromGoogleAPI(mp3folder, book, classification, wordseq, content, div);
		}
		logger.info("==================OptEnglishVoice.excute実行終了！==================");
	}

	private void downloadVoiceFromGoogleAPI(String mp3folder, String book, String classification, String wordseq,
			String content, String div) throws IOException {
		//      String word = "method";  // 指定英语单词
		//      String output = "method.mp3";
		String filePath = mp3folder + "//" + book + "//" + classification + "//";
		String fileName = wordseq + "-" + div + ".mp3";

		File folder = new File(filePath);

		if (!folder.exists()) {
			folder.mkdirs();
		}

		// 构建输入（文本）
		SynthesisInput input = SynthesisInput.newBuilder()
				.setText(content)
				.build();

		// 声音设置（英语，默认女声）
		VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
				.setLanguageCode("ja-JP")
				.setName("ja-JP-Wavenet-A")
				.build();

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
