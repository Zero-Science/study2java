package opt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import common.PropertiesReader;
import db.DBManager;

public class OptExplainENtoJP {
	Logger logger = Logger.getLogger(OptExplainENtoJP.class);

	public void excute(String[] parameter) throws Exception {
		this.logger.info("==================OptJapaneseExplain.excute実行開始！==================");
		String api = parameter[0];
		String book = parameter[1];
		String classification = parameter[2];
		String wordseq = parameter[3];
		String englishContent = parameter[4];
		String div = parameter[5];
		String content = null;
		if ("ExcelAPI".equals(api)) {
			content = getJanpaneseExplainByAPI1(englishContent);
			if (content == null)
				content = getJanpaneseExplainByAPI2(englishContent);
			if (content == null)
				content = getJanpaneseExplainByAPI3(englishContent);
		} else if ("DeepLAPI".equals(api)) {
			content = getJanpaneseExplainByAPI2(englishContent);
			if (content == null)
				content = getJanpaneseExplainByAPI3(englishContent);
		} else if ("googleAPI".equals(api)) {
			content = getJanpaneseExplainByAPI3(englishContent);
		}
		this.logger.info(content);
		if (content == null) {
			this.logger.error("日本語取得失敗しました。[" + api + "][" + book + "][" + classification + "][" + wordseq + "]【"
					+ englishContent + "】");
		} else if (content != null && !"".equals(content)) {
			updateWordInfo(book, classification, wordseq, div, content);
		}
		this.logger.info("==================OptJapaneseExplain.excute実行終了！==================");
	}

	private String getJanpaneseExplainByAPI1(String englishContent) throws Exception {
		URL url = new URL("https://api.excelapi.org/dictionary/enja?word=" + englishContent);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		int responseCode = connection.getResponseCode();
		if (responseCode == 200) {
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), "UTF-8"));
			StringBuilder response = new StringBuilder();
			String inputLine;
			while ((inputLine = bufferedReader.readLine()) != null)
				response.append(inputLine);
			bufferedReader.close();
			return response.toString();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		String errorMessage = in.readLine();
		in.close();
		this.logger.error(errorMessage);
		return null;
	}

	private String getJanpaneseExplainByAPI2(String englishContent) throws DeepLException, InterruptedException {
		DeepLClient client = new DeepLClient(PropertiesReader.JP_EXP_APP_KEY);
		TextResult result = client.translateText(englishContent, "EN", "JA");
		return result.getText();
	}

	private String getJanpaneseExplainByAPI3(String englishContent) throws DeepLException, InterruptedException {
		Translate translate = (Translate) TranslateOptions.getDefaultInstance().getService();
		Translation translation = translate.translate(
				englishContent, new Translate.TranslateOption[] { Translate.TranslateOption.sourceLanguage("en"),
						Translate.TranslateOption.targetLanguage("ja"),
						Translate.TranslateOption.model("base") });
		return translation.getTranslatedText();
	}

	private void updateWordInfo(String book, String classification, String wordseq, String div, String content) {
		StringBuffer sql = new StringBuffer();
		ArrayList<Object> list = new ArrayList<Object>();
		sql.append("UPDATE \"STY_単語情報\" ");
		sql.append("SET ");
		if ("word".equals(div)) {
			sql.append("\"単語_日本語\" = ? ");
			list.add(content);
		} else if ("sen1".equals(div)) {
			sql.append("\"例句1_日本語\" = ? ");
			list.add(content);
		} else if ("sen2".equals(div)) {
			sql.append("\"例句2_日本語\" = ? ");
			list.add(content);
		}
		sql.append("WHERE ");
		sql.append("\"書籍\" = ? AND ");
		list.add(book);
		sql.append("\"分類\" = ? AND ");
		list.add(classification);
		sql.append("\"単語SEQ\" = ? ");
		list.add(Integer.parseInt(wordseq));
		DBManager.update(sql.toString(), list);
		this.logger.info(sql.toString());
	}
}
