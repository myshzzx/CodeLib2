
package mysh.codelib2.model;

import java.io.UnsupportedEncodingException;
import java.util.List;

import mysh.util.FileUtil;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * 导出引擎.
 * 
 * @author Allen
 * 
 */
public class ExportEngine {

	/**
	 * 导出信息.
	 * 
	 * @author Allen
	 */
	public static class ExportInfo {

		/**
		 * 标题.
		 */
		public String title;

		/**
		 * 导出路径.
		 */
		public String filepath;
	}

	/**
	 * 导出数据.
	 * 
	 * @param filepath
	 *               文件路径.
	 * @param eles
	 *               要导出的元素.
	 * @return 不支持的导出类型返回 false.
	 * @throws Exception
	 */
	public static void export(ExportInfo info, List<CodeLib2Element> eles) throws Exception {

		String extention = FileUtil.getFileExtention(info.filepath);

		switch (extention) {
		case "zcl2":
			toZul2(info, eles);
			break;
		case "html":
			toHtml(info, eles);
			break;
		default:
			throw new RuntimeException("不支持的导出类型: " + extention);
		}

	}

	/**
	 * 导出为 zul2 文件.
	 * 
	 * @param filepath
	 * 
	 * @param eles
	 * @throws Exception
	 */
	private static void toZul2(ExportInfo info, List<CodeLib2Element> eles) throws Exception {

		new DataHeader().saveToFile(info.filepath, eles);
	}

	/**
	 * 导出为 html 文件.
	 * 
	 * @param filepath
	 * 
	 * @param eles
	 * @throws UnsupportedEncodingException
	 */
	private static void toHtml(ExportInfo info, List<CodeLib2Element> eles) throws UnsupportedEncodingException {

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html><html><head><meta charset='");
		html.append(CodeLib2Element.DefaultCharsetEncode);
		html.append("'/><title>");
		html.append(info.title);
		html.append("</title><style type='text/css'>#itemlist ul { 	list-style: none; 	margin: 0; 	padding: 0; 	font-family: Arial; 	font-size: 13px; } #itemlist li { 	cursor: pointer; 	margin: 1px 0px 1px 0px; 	background-color: #EDF2F8; 	border-style: solid; 	border-width: 1px; 	color: #376BAD; 	border-color: #EDF5FD; } #itemlist li div:hover { 	background-color: #BBCEE6; 	border-style: solid; 	border-width: 0px; } .hideDiv { 	visibility: hidden; }</style><script type='text/javascript'>function show(index, li) {	var keywords = document.getElementById('keywords');	keywords.value = li.outerText;		var text = document.getElementById('text');	var value = document.getElementById('i' + index).innerHTML;	text.innerHTML = value; 	}</script></head><body style='position: fixed; width: 100%; height: 100%;'><div	style='position: absolute; text-align: center; width: 100%; height: 15%;'><h1>");
		html.append(info.title);
		html.append("</h1></div><div style='position: absolute; top: 15%; width: 100%; height: 85%;'><div id='itemlist' style='position: absolute; width: 30%; height: 95%; overflow-y: auto;'><ul>");

		int itemIndex = 0;
		for (CodeLib2Element ele : eles) {
			itemIndex++;
			html.append("<li onclick='show(");
			html.append(itemIndex);
			html.append(", this)'><div>");
			html.append(StringEscapeUtils.escapeHtml4(ele.getKeywords()));
			html.append("</div></li>");
		}

		html.append("</ul></div><div style='position: absolute; width: 65%; height: 95%; left: 32%;'><input id='keywords' readonly='readonly' style='width: 100%' /><textarea id='text' readonly='readonly'	style='width: 100%; height: 90%;'> </textarea></div></div>");

		itemIndex = 0;
		for (CodeLib2Element ele : eles) {
			itemIndex++;
			html.append("<div id='i");
			html.append(itemIndex);
			html.append("' class='hideDiv'>");
			html.append(StringEscapeUtils.escapeHtml4(new String(ele.getContent(),
					CodeLib2Element.DefaultCharsetEncode)));
			html.append("</div>");
		}

		html.append("</body></html>");

		FileUtil.writeFile(info.filepath, html.toString().getBytes(CodeLib2Element.DefaultCharsetEncode));
	}
}
