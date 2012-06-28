
package mysh.codelib2.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import mysh.util.TextEncodeUtil;

/**
 * 代码库元素.<br/>
 * 定义代码库存储单元.
 * 
 * @author Allen
 * 
 */
public class CodeLib2Element implements Serializable, Comparable<CodeLib2Element> {

	private static final long serialVersionUID = -1657793191602091756L;

	/**
	 * 附件.
	 * 
	 * @author Allen
	 * 
	 */
	public static class Attachment implements Serializable, Comparable<Attachment> {

		private static final long serialVersionUID = -3905992950416299776L;

		/**
		 * 内容类型.
		 * 
		 * @author Allen
		 * 
		 */
		public static enum ContentType {
			/**
			 * 二进制.
			 */
			Binary, /**
			 * UTF8 文本.
			 */
			UTF8Text, /**
			 * 非 UTF8 文本.
			 */
			NonUTF8Text;

			/**
			 * 文本类型扩展名.
			 */
			private static List<String> textExt = Arrays.asList("as", "asp", "bat", "c",
					"cpp", "cs", "css", "h", "html", "htm", "php", "pl", "ini",
					"java", "js", "jsp", "log", "lua", "mx", "mxml", "pas",
					"properties", "py", "sql", "sh", "txt", "vb", "vbs", "xml",
					"xsd", "xsl");
		}

		/**
		 * 附件名.
		 */
		private String name;

		/**
		 * 附件内容.
		 */
		private byte[] binaryContent;

		/**
		 * 附件内容类型.
		 */
		private ContentType contentType = ContentType.Binary;

		@Override
		public boolean equals(Object obj) {

			if (obj instanceof Attachment) {
				Attachment a = (Attachment) obj;
				boolean flag = true;

				if (this.name != null)
					flag &= this.name.equals(a.name);
				else if (a.name != null)
					return false;

				flag &= Arrays.equals(this.binaryContent, a.binaryContent);

				return flag;
			}
			return false;
		}

		@Override
		public int compareTo(Attachment o) {

			if (this.name == o.name)
				return 0;
			else if (this.name == null)
				return -1;
			else if (o.name == null)
				return 1;
			else
				return this.name.compareTo(o.name);
		}

		/**
		 * 附件名.
		 * 
		 * @return
		 */
		public String getName() {

			return name;
		}

		/**
		 * 附件名.
		 * 
		 * @param name
		 */
		public void setName(String name) {

			this.name = name;
			this.judgeContentType();
		}

		/**
		 * 内容类型.
		 * 
		 * @return
		 */
		public ContentType getContentType() {

			return this.contentType;
		}

		/**
		 * 判断内容类型.
		 */
		private void judgeContentType() {

			this.contentType = ContentType.Binary;
			
			if (this.binaryContent != null && this.binaryContent.length > 0
					&& this.name != null && this.name.length() > 0) {
				int pointPos = this.name.lastIndexOf('.');
				if (pointPos > -1 && pointPos < this.name.length() - 1) {
					String ext = this.name.substring(pointPos + 1,
							this.name.length());

					if (ContentType.textExt.contains(ext)) {
						if (TextEncodeUtil.isUTF8Bytes(this.binaryContent)) {
							this.contentType = ContentType.UTF8Text;
						} else {
							this.contentType = ContentType.NonUTF8Text;
						}
					}
				}
			}
		}

		/**
		 * 附件内容.
		 * 
		 * @return
		 */
		public byte[] getBinaryContent() {

			return binaryContent;
		}

		/**
		 * 附件内容.
		 * 
		 * @param binaryContent
		 */
		public void setBinaryContent(byte[] binaryContent) {

			this.binaryContent = binaryContent;
			this.judgeContentType();
		}

	}

	/**
	 * 关键字.
	 */
	private String keywords = "";

	/**
	 * 内容.
	 */
	private byte[] content;

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof CodeLib2Element) {
			CodeLib2Element e = (CodeLib2Element) obj;
			boolean flag = true;

			if (this.keywords != null) {
				flag &= this.keywords.equals(e.keywords);
			} else if (e.keywords != null) {
				return false;
			}

			flag &= Arrays.equals(this.content, e.content);

			return flag;
		}

		return false;
	}

	@Override
	public int compareTo(CodeLib2Element o) {

		if (this.keywords == o.keywords) {
			return 0;
		} else if (this.keywords == null) {
			return -1;
		} else if (o.keywords == null) {
			return 1;
		} else {
			return this.keywords.compareTo(o.keywords);
		}
	}

	/**
	 * 关键字.
	 * 
	 * @return
	 */
	public String getKeywords() {

		return keywords;
	}

	/**
	 * 关键字.
	 * 
	 * @param keywords
	 */
	public final CodeLib2Element setKeywords(String keywords) {

		if (keywords == null) {
			this.keywords = "";
		} else {
			StringBuilder r = new StringBuilder();
			String[] keys = keywords.trim().split(",");
			for (String key : keys) {
				key = key.trim();
				if (key.length() > 0) {
					r.append(key);
					r.append(", ");
				}
			}
			if (r.length() > 2) {
				this.keywords = r.substring(0, r.length() - 2);
			} else {
				this.keywords = "";
			}
		}

		return this;
	}

	/**
	 * 内容.
	 * 
	 * @return
	 */
	public byte[] getContent() {

		return content;
	}

	/**
	 * 内容.
	 * 
	 * @param content
	 */
	public void setContent(byte[] content) {

		this.content = content;
	}

}
