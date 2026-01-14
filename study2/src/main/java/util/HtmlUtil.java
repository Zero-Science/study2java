package util;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class HtmlUtil {

	public static String toHtml(String markdown) {

		Parser parser = Parser.builder().build();

		HtmlRenderer renderer = HtmlRenderer.builder().build();

		return renderer.render(parser.parse(markdown));

	}

}
