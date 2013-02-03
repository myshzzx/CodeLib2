
package mysh.codelib2.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import mysh.util.ByteUtil;

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
	 * 默认文本编码格式.
	 */
	public static final String DefaultCharsetEncode = ByteUtil.DefaultEncode;

	/**
	 * 默认关键字.
	 */
	private static final String DefaultKeywords = " ";

	/**
	 * 附件.
	 * 
	 * @author Allen
	 * 
	 */
	public static class Attachment implements Serializable, Comparable<Attachment> {

		private static final long serialVersionUID = -3905992950416299776L;

		// /**
		// * 内容类型.
		// *
		// * @author Allen
		// *
		// */
		// public static enum ContentType {
		// /**
		// * 二进制.
		// */
		// Binary, /**
		// * UTF8 文本.
		// */
		// UTF8Text, /**
		// * 非 UTF8 文本.
		// */
		// NonUTF8Text;
		//
		// /**
		// * 支持的文本类型扩展名.
		// */
		// private static final List<String> textExt = Arrays.asList("as", "asm", "asp",
		// "bat", "bbcode",
		// "c", "clj", "clojure", "cpp", "cs", "css", "f", "for", "fortran", "groovy", "gsp",
		// "h", "htm", "html", "ini", "java", "js", "jsp", "lisp", "log", "lua", "mx", "mxml",
		// "pas", "php", "pl", "properties", "py", "rb", "ruby", "sas", "scala", "sh", "sql",
		// "tcl", "txt", "vb", "vbs", "xml", "xsd", "xsl");
		//
		// /**
		// * 取文本编码类型. 若非文本, 返回 null.
		// *
		// * @return
		// */
		// public String getTextEncode() {
		//
		// switch (this) {
		// case UTF8Text:
		// return "UTF-8";
		// case NonUTF8Text:
		// return "GBK";
		// default:
		// return null;
		// }
		// }
		// }

		/**
		 * 附件名.
		 */
		private String name;

		/**
		 * 附件内容.
		 */
		private byte[] binaryContent;

		// /**
		// * 附件内容类型.
		// */
		// private ContentType contentType = ContentType.Binary;

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
		public int hashCode() {

			return this.name.hashCode();
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

		@Override
		public String toString() {

			return this.name;
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
		public Attachment setName(String name) {

			this.name = name;
			// this.judgeContentType();
			return this;
		}

		// /**
		// * 内容类型.
		// *
		// * @return
		// */
		// public ContentType getContentType() {
		//
		// return this.contentType;
		// }
		//
		// /**
		// * 判断内容类型.
		// */
		// private void judgeContentType() {
		//
		// this.contentType = ContentType.Binary;
		//
		// if (this.binaryContent != null && this.binaryContent.length > 0 && this.name !=
		// null
		// && this.name.length() > 0) {
		// int pointPos = this.name.lastIndexOf('.');
		// if (pointPos > -1 && pointPos < this.name.length() - 1) {
		// String ext = this.name.substring(pointPos + 1, this.name.length());
		//
		// if (ContentType.textExt.contains(ext)) {
		// if (TextEncodeUtil.isUTF8Bytes(this.binaryContent)) {
		// this.contentType = ContentType.UTF8Text;
		// } else {
		// this.contentType = ContentType.NonUTF8Text;
		// }
		// }
		// }
		// }
		// }

		/**
		 * 附件内容.
		 * 
		 * @return
		 */
		public byte[] getBinaryContent() {

			return this.binaryContent == null ? new byte[0] : this.binaryContent;
		}

		/**
		 * 附件内容.
		 * 
		 * @param binaryContent
		 */
		public Attachment setBinaryContent(byte[] binaryContent) {

			this.binaryContent = binaryContent;
			// this.judgeContentType();
			return this;
		}

	}

	/**
	 * 关键字. (保证不为 null)
	 */
	private String keywords = DefaultKeywords;

	/**
	 * 内容.
	 */
	private byte[] content;

	/**
	 * 附件.
	 */
	private List<Attachment> attachments;

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

			flag &= this.content == e.content || Arrays.equals(this.content, e.content);

			return flag;
		}

		return false;
	}

	@Override
	public int hashCode() {

		return this.keywords.hashCode();
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

	@Override
	public String toString() {

		if (this.attachments == null || this.attachments.size() == 0) {
			return this.keywords;
		} else {
			return this.keywords + " [附]";
		}
	}

	/**
	 * 关键字.
	 * 
	 * @return
	 */
	public String getKeywords() {

		return this.keywords == null ? DefaultKeywords : this.keywords;
	}

	/**
	 * 关键字.
	 * 
	 * @param keywords
	 */
	public final CodeLib2Element setKeywords(String keywords) {

		if (keywords == null) {
			this.keywords = DefaultKeywords;
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
				this.keywords = DefaultKeywords;
			}
		}

		return this;
	}

	/**
	 * 取第一个关键字.
	 * 
	 * @return
	 */
	public String getFirstKeyword() {

		return this.keywords.split(",")[0];
	}

	/**
	 * 附件.
	 * 
	 * @return
	 */
	public List<Attachment> getAttachments() {

		return attachments;
	}

	/**
	 * 附件.
	 * 
	 * @param attachments
	 */
	public void setAttachments(List<Attachment> attachments) {

		this.attachments = attachments;
	}

	/**
	 * 内容.
	 * 
	 * @return
	 */
	public byte[] getContent() {

		if (this.content == null) {
			return new byte[0];
		}
		return content;
	}

	/**
	 * 内容.
	 * 
	 * @param content
	 * @return
	 */
	public void setContent(byte[] content) {

		this.content = content;
	}

}
