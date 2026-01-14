package start;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import common.PropertiesReader;

public class Start {

	private static Logger logger = Logger.getLogger(Start.class);

	public static PropertiesReader propertiesReader;

	public static void main(String[] args) {

		PropertyConfigurator.configure("/usr/local/tomcat/java/log4j.properties");
		PropertiesReader.configure("/usr/local/tomcat/java/config.properties");

		//		  PropertyConfigurator.configure("/usr/local/tomcat/webapps/study3/java/log4j.properties");
		//		  PropertiesReader.configure("/usr/local/tomcat/webapps/study3/java/config.properties");

		//		  PropertyConfigurator.configure("D:\\config\\log4j.properties");
		//		  PropertiesReader.configure("D:\\config\\config.properties");

		//		  args = new String[8];
		//		  args[0] = "OptVoiceJP";
		//		  args[1] = "googleAPI";
		//		  args[2] = "D://mp3";
		//		  args[3] = "111";
		//		  args[4] = "222";
		//		  args[5] = "666";
		//		  args[6] = "東京都";
		//		  args[7] = "word";

		logger.info("#####################################################");
		logger.info(Arrays.toString(args));

		String opt = args[0];
		String[] parameter = getParameter(args);
		excute(opt, parameter);
		logger.info("#####################################################");

	}

	private static void excute(String opt, String[] parameter) {
		try {
			Class<?> clazz = Class.forName("opt." + opt);
			Object instance = clazz.newInstance();
			Method method = clazz.getMethod("excute", new Class[] { String[].class });
			method.invoke(instance, new Object[] { parameter });
		} catch (Exception e) {

			logger.error("システムエラー！", e);
			e.printStackTrace();
		}
	}

	private static String[] getParameter(String[] args) {
		if (args.length <= 1)
			return null;
		String[] newArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++)
			newArgs[i - 1] = args[i];
		return newArgs;
	}

}
