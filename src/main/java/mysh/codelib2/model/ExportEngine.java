
package mysh.codelib2.model;

import java.util.List;

import mysh.util.FileUtil;

/**
 * 导出引擎.
 * 
 * @author Allen
 * 
 */
public class ExportEngine {

	/**
	 * 导出数据.
	 * 
	 * @param filepath
	 * @param eles
	 * @return 不支持的导出类型返回 false.
	 * @throws Exception
	 */
	public static void export(String filepath, List<CodeLib2Element> eles) throws Exception {

		String extention = FileUtil.getFileExtention(filepath);

		switch (extention) {
		case "zcl2":
			toZul2(filepath, eles);
			break;
		case "html":
			toHtml(filepath, eles);
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
	private static void toZul2(String filepath, List<CodeLib2Element> eles) throws Exception {

		new DataHeader().saveToFile(filepath, eles);
	}

	/**
	 * 导出为 html 文件.
	 * 
	 * @param filepath
	 * 
	 * @param eles
	 */
	private static void toHtml(String filepath, List<CodeLib2Element> eles) {

	}
}
