package opt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import db.DBManager;

public class OptExplainENtoCH {
	Logger logger = Logger.getLogger(OptExplainENtoCH.class);

	public void excute(String[] parameter) throws Exception {
		this.logger.info("==================OptChineseExplain.excute実行開始！==================");
		String api = parameter[0];
		String book = parameter[1];
		String classification = parameter[2];
		String wordseq = parameter[3];
		String englishContent = parameter[4];
		String div = parameter[5];
		String content = null;
		if ("youdaoAPI".equals(api)) {
			content = getChineseExplainByAPI1(englishContent);
			if (content == null)
				content = getChineseExplainByAPI2(englishContent);
		} else if ("googleAPI".equals(api)) {
			content = getChineseExplainByAPI2(englishContent);
		}
		this.logger.info(content);
		if (content == null)
			this.logger.error("中国語取得失敗しました。[" + api + "][" + book + "][" + classification + "][" + wordseq + "]【"
					+ englishContent + "】");
		if (content != null && !"".equals(content)) {
			if (content.length() > 500)
				content = String.valueOf(content.substring(0, 497)) + "...";
			updateWordInfo(book, classification, wordseq, div, content);
		}
		this.logger.info("==================OptChineseExplain.excute実行終了！==================");
	}

	private String getChineseExplainByAPI1(String englishContent) throws IOException {
		englishContent = englishContent.replaceAll(" ", "%20").replaceAll("'", "%27").replaceAll("～", "~");
		URL url = new URL("http://dict.youdao.com/suggest?num=1&doctype=json&q=" + englishContent);
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
			JSONObject json = new JSONObject(response.toString());
			String msg = json.getJSONObject("result").getString("msg");
			int code = json.getJSONObject("result").getInt("code");
			if ("success".equals(msg) && code == 200)
				return json.getJSONObject("data").getJSONArray("entries").getJSONObject(0).getString("explain");
			return null;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		String errorMessage = in.readLine();
		in.close();
		this.logger.error(errorMessage);
		return null;
	}

	private String getChineseExplainByAPI2(String englishContent) throws Exception {
		Translate translate = (Translate) TranslateOptions.getDefaultInstance().getService();
		Translation translation = translate.translate(
				englishContent, new Translate.TranslateOption[] { Translate.TranslateOption.sourceLanguage("en"),
						Translate.TranslateOption.targetLanguage("zh"),
						Translate.TranslateOption.model("base") });
		return translation.getTranslatedText();
	}

	private void updateWordInfo(String book, String classification, String wordseq, String div, String content) {
		StringBuffer sql = new StringBuffer();
		ArrayList<Object> list = new ArrayList<Object>();
		sql.append("UPDATE \"STY_単語情報\" ");
		sql.append("SET ");
		if ("word".equals(div)) {
			sql.append("\"単語_中国語\" = ? ");
			list.add(content);
		} else if ("sen1".equals(div)) {
			sql.append("\"例句1_中国語\" = ? ");
			list.add(content);
		} else if ("sen2".equals(div)) {
			sql.append("\"例句2_中国語\" = ? ");
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
