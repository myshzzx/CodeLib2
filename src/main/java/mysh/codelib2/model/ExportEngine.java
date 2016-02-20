
package mysh.codelib2.model;

import mysh.util.FilesUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 导出引擎.
 *
 * @author Allen
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
		public File file;
	}

	/**
	 * 导出数据.
	 *
	 * @param info 导出信息.
	 * @param eles 要导出的元素.
	 * @throws Exception 不支持的导出类型返回 .
	 */
	public static void export(ExportInfo info, List<CodeLib2Element> eles) throws Exception {

		Collections.sort(eles);

		String extention = FilesUtil.getFileExtension(info.file);

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
	 */
	private static void toZul2(ExportInfo info, List<CodeLib2Element> eles) throws Exception {

		new DataHeader().saveToFile(info.file, eles);
	}

	/**
	 * 导出为 html 文件.
	 */
	private static void toHtml(ExportInfo info, List<CodeLib2Element> eles) throws IOException {

		try (FileOutputStream htmlOut = new FileOutputStream(info.file)) {
			htmlOut.write("<!DOCTYPE html><html><head><meta charset='".getBytes(CodeLib2Element.DefaultCharsetEncode));
			htmlOut.write(CodeLib2Element.DefaultCharsetEncode.getBytes(CodeLib2Element.DefaultCharsetEncode));
			htmlOut.write("' /><script>var title = '".getBytes(CodeLib2Element.DefaultCharsetEncode));
			htmlOut.write(info.title.getBytes(CodeLib2Element.DefaultCharsetEncode));
			htmlOut.write("';var keys=[".getBytes(CodeLib2Element.DefaultCharsetEncode));

			int index;
			CodeLib2Element ele;
			int len = eles.size();
			for (index = 0; index < len; index++) {
				ele = eles.get(index);
				if (index > 0)
					htmlOut.write(',');

				htmlOut.write('\'');
				htmlOut.write(Base64.encodeBase64(ele.getKeywords().getBytes(
								CodeLib2Element.DefaultCharsetEncode)));
				htmlOut.write('\'');
			}

			htmlOut.write("];var datas=[".getBytes(CodeLib2Element.DefaultCharsetEncode));

			for (index = 0; index < len; index++) {
				ele = eles.get(index);
				if (index > 0)
					htmlOut.write(',');

				htmlOut.write('\'');
				htmlOut.write(Base64.encodeBase64(ele.getContent()));
				htmlOut.write('\'');
			}

			htmlOut.write("];".getBytes(CodeLib2Element.DefaultCharsetEncode));

			// read compressed html and write to target.
			InputStream tempInput = ExportEngine.class.getResourceAsStream("/html/minimized/html.html");
			final byte[] tempBuf = new byte[1_000_000];
			int tempLen;
			while ((tempLen = tempInput.read(tempBuf)) > 0) {
				htmlOut.write(tempBuf, 0, tempLen);
			}

			htmlOut.flush();
		}
	}
}
