package ai.common;

import java.util.ArrayList;
import java.util.HashMap;

public interface AiClient {
	String call(ArrayList<HashMap<String, Object>> result,String prompt)throws Exception;
	String call(String prompt);
}
