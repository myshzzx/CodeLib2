
package mysh.codelib2.model;

import mysh.collect.Pair;
import mysh.util.Compresses;
import mysh.util.FilesUtil;
import mysh.util.Serializer;
import mysh.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 文件数据头部. <br/>
 * 描述数据基本信息(如是否压缩, 是否加密). <br/>
 *
 * @author Allen
 */
public class DataHeader implements Serializable {
	private static final long serialVersionUID = -5817161670435220173L;
	
	private static final Logger log = LoggerFactory.getLogger(DataHeader.class);
	
	private final int version;
	
	public DataHeader() {
		version = 5;
	}
	
	public DataHeader(int version) {
		this.version = version;
	}
	
	/**
	 * 数据保存到文件.<br/>
	 *
	 * @param file 文件
	 * @param eles 数据集.
	 */
	public static boolean saveToFile(File file, Collection<CodeLib2Element> eles) throws Exception {
		return writeVer5(file, eles);
	}
	
	/**
	 * 从文件读取数据.
	 */
	@SuppressWarnings("unchecked")
	public static Pair<DataHeader, Collection<CodeLib2Element>> readFromFile(File file) throws Exception {
		try (FileInputStream in = new FileInputStream(file)) {
			return readFromInputStream(in);
		}
	}
	
	@NotNull
	public static Pair<DataHeader, Collection<CodeLib2Element>> readFromInputStream(InputStream in) throws Exception {
		DataHeader header = Serializer.BUILD_IN.deserialize(in);
		switch (header.version) {
			case 0:
			case 2:
				return Pair.of(header, check(readVer2(in)));
			case 3:
				return Pair.of(header, check(readVer3(in)));
			case 4:
				return Pair.of(header, check(readVer4(in)));
			case 5:
				return Pair.of(header, check(readVer5(in)));
			default:
				throw new RuntimeException("unknown header version: " + header.version);
		}
	}
	
	private static Collection<CodeLib2Element> check(Collection<CodeLib2Element> items) {
		if (items != null)
			for (CodeLib2Element item : items) {
				if (Strings.isBlank(item.getId()))
					item.setId(UUID.randomUUID().toString());
			}
		
		return items;
	}
	
	private static boolean writeVer5(File file, Collection<CodeLib2Element> eles) throws Exception {
		file.getParentFile().mkdirs();
		File writeFile = FilesUtil.getWriteFile(file);
		try (FileOutputStream out = new FileOutputStream(writeFile)) {
			out.write(Serializer.BUILD_IN.serialize(new DataHeader()));
			out.write(Compresses.compressXz(Serializer.BUILD_IN.serialize(eles)));
		}
		file.delete();
		return writeFile.renameTo(file);
	}
	
	private static Collection<CodeLib2Element> readVer5(InputStream in) throws Exception {
		return Serializer.BUILD_IN.deserialize(Compresses.decompressXz(in.readAllBytes()));
	}
	
	private static Collection<CodeLib2Element> readVer4(InputStream in) throws Exception {
		AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress((entry, ein) -> result.set(Serializer.BUILD_IN.deserialize(ein)), in);
		return result.get();
	}
	
	private static Collection<CodeLib2Element> readVer3(InputStream in) throws Exception {
		AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress((entry, ein) -> result.set(Serializer.FST.deserialize(ein)), in);
		return result.get();
	}
	
	private static Collection<CodeLib2Element> readVer2(InputStream in) throws Exception {
		final AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress(
				(entry, ein) -> {
					try {
						ObjectInputStream objIn = new ObjectInputStream(ein);
						result.set((Collection<CodeLib2Element>) objIn.readObject());
					} catch (Exception e) {
						throw new RuntimeException("解压失败: " + e);
					}
				}, in);
		return result.get();
	}
	
}
