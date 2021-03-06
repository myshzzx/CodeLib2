
package mysh.codelib2.model;

import mysh.collect.Colls;
import mysh.util.Bytes;
import mysh.util.Encodings;
import mysh.util.Times;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 代码库元素.<br/>
 * 定义代码库存储单元.
 *
 * @author Allen
 */
public class CodeLib2Element implements Serializable, Comparable<CodeLib2Element> {
	
	private static final long serialVersionUID = -1657793191602091756L;
	
	/**
	 * 默认文本编码格式.
	 */
	public static final String DefaultCharsetEncode = Bytes.DefaultEncode;
	
	/**
	 * 默认关键字.
	 */
	private static final String DefaultKeywords = " ";
	
	/**
	 * 附件.
	 *
	 * @author Allen
	 */
	public static class Attachment implements Serializable, Comparable<Attachment> {
		
		private static final long serialVersionUID = -3905992950416299776L;
		
		/**
		 * 内容类型.
		 *
		 * @author Allen
		 */
		public static enum ContentType {
			/**
			 * 二进制.
			 */
			Binary,
			/**
			 * UTF8 文本.
			 */
			UTF8Text,
			/**
			 * 非 UTF8 文本.
			 */
			NonUTF8Text,
			/**
			 * image
			 */
			Img;
			
			/**
			 * 支持的文本类型扩展名.
			 */
			private static final Set<String> textExt = Colls.ofHashSet("as", "asm", "asp", "bat", "bbcode",
					"c", "clj", "clojure", "cpp", "cs", "css", "d", "f", "for", "fortran", "groovy", "gsp",
					"h", "htm", "html", "ini", "java", "js", "jsp", "lisp", "log", "lua", "mq4", "mq5", "mqh", "mx",
					"mxml", "pas", "php", "pl", "properties", "py", "r", "rb", "reg", "ruby", "sas", "scala", "scheme",
					"scm", "sh", "sql", "ss", "tcl", "txt", "vb", "vbs", "xml", "xsd", "xsl");
			
			private static final Set<String> imgExt = Colls.ofHashSet("jpg", "jpeg", "gif", "png", "bmp",
					"svg", "jp2");
			
			/**
			 * 取文本编码类型. 若非文本, 返回 null.
			 */
			public String getTextEncode() {
				
				switch (this) {
					case UTF8Text:
						return "UTF-8";
					case NonUTF8Text:
						return "GBK";
					default:
						return null;
				}
			}
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
		 * 不持久化是为了将来支持新的文件类型时, 内容类型可以被正确设置.
		 */
		private transient ContentType contentType;
		
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
		 */
		public String getName() {
			
			return name;
		}
		
		/**
		 * 附件名.
		 */
		public Attachment setName(String name) {
			
			this.name = name;
			// this.judgeContentType();
			return this;
		}
		
		/**
		 * 内容类型.
		 */
		public ContentType getContentType() {
			if (this.contentType == null) {
				this.judgeContentType();
			}
			return this.contentType;
		}
		
		/**
		 * 判断内容类型.
		 */
		public void judgeContentType() {
			this.contentType = ContentType.Binary;
			
			if (this.binaryContent != null && this.binaryContent.length > 0
					&& this.name != null && this.name.length() > 0) {
				int pointPos = this.name.lastIndexOf('.');
				if (pointPos > -1 && pointPos < this.name.length() - 1) {
					String ext = this.name.substring(pointPos + 1).toLowerCase();
					
					if (ContentType.textExt.contains(ext)) {
						if (Encodings.isUTF8Bytes(this.binaryContent)) {
							this.contentType = ContentType.UTF8Text;
						} else {
							this.contentType = ContentType.NonUTF8Text;
						}
					} else if (ContentType.imgExt.contains(ext)) {
						this.contentType = ContentType.Img;
					}
				}
			}
		}
		
		/**
		 * 附件内容.
		 */
		public byte[] getBinaryContent() {
			return this.binaryContent == null ? new byte[0] : this.binaryContent;
		}
		
		/**
		 * 附件内容.
		 */
		public Attachment setBinaryContent(byte[] binaryContent) {
			this.binaryContent = binaryContent;
			this.judgeContentType();
			return this;
		}
		
	}
	
	private String id = UUID.randomUUID().toString();
	
	/**
	 * 关键字. (代码范围内保证不为 null, 但反射和反序列化仍可能导致 null)
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
	
	private Instant createTime = Instant.now(), updateTime = createTime;
	
	private boolean deleted;
	
	@Override
	public boolean equals(Object obj) {
		
		if (!(obj instanceof CodeLib2Element))
			return false;
		
		CodeLib2Element e = (CodeLib2Element) obj;
		return Objects.equals(id, e.id);
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
	
	public String showInfo() {
		return "创建时间: "
				+
				Times.format(Times.Formats.DayTime, createTime, ZoneId.systemDefault())
				+
				"\n修改时间: "
				+
				Times.format(Times.Formats.DayTime, updateTime, ZoneId.systemDefault());
	}
	
	public void delete() {
		updateTime = Instant.now();
		deleted = true;
		keywords = DefaultKeywords;
		content = null;
		attachments = null;
	}
	
	// get set
	
	public String getId() {
		return id;
	}
	
	public CodeLib2Element setId(final String id) {
		this.id = id;
		return this;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	/**
	 * 关键字.
	 */
	public String getKeywords() {
		
		return this.keywords == null ? DefaultKeywords : this.keywords;
	}
	
	/**
	 * 关键字.
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
		
		updateTime = Instant.now();
		return this;
	}
	
	/**
	 * 取第一个关键字.
	 */
	public String getFirstKeyword() {
		
		return this.keywords.split(",")[0];
	}
	
	/**
	 * 附件.
	 */
	public List<Attachment> getAttachments() {
		
		return attachments;
	}
	
	/**
	 * 附件.
	 */
	public void setAttachments(List<Attachment> attachments) {
		
		this.attachments = attachments;
		this.updateTime = Instant.now();
	}
	
	/**
	 * 内容.
	 */
	public byte[] getContent() {
		
		if (this.content == null) {
			return new byte[0];
		}
		return content;
	}
	
	/**
	 * 内容.
	 */
	public void setContent(byte[] content) {
		
		this.content = content;
		this.updateTime = Instant.now();
	}
	
	public Instant getCreateTime() {
		return createTime;
	}
	
	public CodeLib2Element setCreateTime(Instant createTime) {
		this.createTime = createTime;
		return this;
	}
	
	public Instant getUpdateTime() {
		return updateTime;
	}
	
	public CodeLib2Element setUpdateTime(Instant updateTime) {
		this.updateTime = updateTime;
		return this;
	}
	
	public int getSize() {
		int s = keywords.length();
		if (content != null)
			s += content.length;
		if (Colls.isNotEmpty(attachments))
			for (Attachment attachment : attachments) {
				s += attachment.binaryContent.length;
			}
		return s;
	}
}
