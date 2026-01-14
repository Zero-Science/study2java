package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import common.PropertiesReader;

public class DBManager {

	static Logger logger = Logger.getLogger(DBManager.class);

	public static Connection connection = null;

	public static void connect() {

		try {
			if (connection == null || connection.isClosed()) {
				Class.forName("org.postgresql.Driver");
				connection = DriverManager.getConnection(PropertiesReader.DB_URL, PropertiesReader.USER,
						PropertiesReader.PASS);
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("DB接続時にエラー発生しました！", e);
			e.printStackTrace();
		}

	}

	public static ArrayList<HashMap<String, Object>> select(String paramString) {

		PreparedStatement preparedStatement = null;
		ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
		try {
			connect();
			preparedStatement = connection.prepareStatement(paramString);
			ResultSet resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

			int i = resultSetMetaData.getColumnCount();
			while (resultSet.next()) {

				HashMap<String, Object> hashMap = new HashMap<>();
				byte b;
				for (b = 1; b <= i; b = (byte) (b + 1)) {
					hashMap.put(resultSetMetaData.getColumnName(b), resultSet.getObject(b));
				}
				arrayList.add(hashMap);
			}
		} catch (Exception exception) {

			logger.error("DB検索時にエラー発生しました！", exception);
			exception.printStackTrace();
		} finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
				if (connection != null)
					connection.close();
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
		return arrayList;
	}

	public static void update(String paramString) {

		if ("on".equals(PropertiesReader.UPD_FLG)) {
			PreparedStatement preparedStatement = null;

			try {
				connect();
				preparedStatement = connection.prepareStatement(paramString);
				preparedStatement.executeUpdate();
			} catch (Exception exception) {
				logger.error("DB更新時にエラー発生しました！", exception);
				exception.printStackTrace();
			} finally {
				try {
					if (preparedStatement != null)
						preparedStatement.close();
					if (connection != null)
						connection.close();
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
		}

	}

	public static void update(String paramString, ArrayList<?> list) {
		if ("on".equals(PropertiesReader.UPD_FLG)) {
			PreparedStatement preparedStatement = null;
			logger.info(paramString);
			try {
				connect();
				preparedStatement = connection.prepareStatement(paramString);

				for (int i = 0; i < list.size(); i++) {

					Object param = list.get(i);

					if (param instanceof String) {
						preparedStatement.setString(i + 1, (String) param);

						logger.info(param);
						continue;
					} else if (param instanceof Integer) {
						preparedStatement.setInt(i + 1, (int) param);

						logger.info(param);
						continue;
					} else {
						preparedStatement.setObject(i + 1, param);

						logger.info(param);
						continue;
					}

				}

				preparedStatement.executeUpdate();

			} catch (Exception exception) {
				logger.error("DB更新時にエラー発生しました！", exception);
				exception.printStackTrace();
			} finally {
				try {
					if (preparedStatement != null)
						preparedStatement.close();
					if (connection != null)
						connection.close();
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}

		}
	}

	public static ArrayList<HashMap<String, Object>> select(String paramString, ArrayList<?> list) {

		PreparedStatement preparedStatement = null;
		ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
		try {
			connect();
			preparedStatement = connection.prepareStatement(paramString);

			for (int i = 0; i < list.size(); i++) {

				Object param = list.get(i);

				if (param instanceof String) {
					preparedStatement.setString(i + 1, (String) param);

					logger.info(param);
					continue;
				} else if (param instanceof Integer) {
					preparedStatement.setInt(i + 1, (int) param);

					logger.info(param);
					continue;
				} else {
					preparedStatement.setObject(i + 1, param);

					logger.info(param);
					continue;
				}

			}

			ResultSet resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

			int i = resultSetMetaData.getColumnCount();
			while (resultSet.next()) {

				HashMap<String, Object> hashMap = new HashMap<>();
				byte b;
				for (b = 1; b <= i; b = (byte) (b + 1)) {
					hashMap.put(resultSetMetaData.getColumnName(b), resultSet.getObject(b));
				}
				arrayList.add(hashMap);
			}
		} catch (Exception exception) {

			logger.error("DB検索時にエラー発生しました！", exception);
			exception.printStackTrace();
		} finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
				if (connection != null)
					connection.close();
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
		return arrayList;
	}

}
