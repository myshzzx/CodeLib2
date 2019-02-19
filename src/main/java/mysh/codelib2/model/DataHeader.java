
package mysh.codelib2.model;

import mysh.collect.Pair;
import mysh.util.Compresses;
import mysh.util.FilesUtil;
import mysh.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
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

	private int version;

	public DataHeader(int version) {
		this.version = version;
	}

	/**
	 * 数据保存到文件.<br/>
	 *
	 * @param file 文件
	 * @param eles 数据集.
	 */
	public boolean saveToFile(File file, Collection<CodeLib2Element> eles) throws Exception {
		version = 4;
		return writeVer4(file, eles);
	}

	/**
	 * 从文件读取数据.
	 */
	@SuppressWarnings("unchecked")
	public static Pair<DataHeader, Collection<CodeLib2Element>> readFromFile(File file) throws Exception {
		try (FileInputStream in = new FileInputStream(file)) {
			DataHeader header = readHeader(in);
			switch (header.version) {
				case 0:
				case 2:
					return Pair.of(header, readVer2(in));
				case 3:
					return Pair.of(header, readVer3(in));
				case 4:
					return Pair.of(header, readVer4(in));
				default:
					throw new RuntimeException("unknown header version: " + header.version);
			}
		}
	}

	private void writeHeader(OutputStream out) throws IOException {
		out.write(Serializer.BUILD_IN.serialize(this));
		out.flush();
	}

	private static DataHeader readHeader(InputStream in) {
		return Serializer.BUILD_IN.deserialize(in);
	}

	private boolean writeVer4(File file, Collection<CodeLib2Element> eles) throws Exception {
		file.getParentFile().mkdirs();
		File writeFile = FilesUtil.getWriteFile(file);
		try (FileOutputStream out = new FileOutputStream(writeFile)) {
			writeHeader(out);
			Compresses.compress("zcl", new ByteArrayInputStream(Serializer.BUILD_IN.serialize((Serializable) eles)),
							Long.MAX_VALUE, out, 500_000);
		}
		file.delete();
		return writeFile.renameTo(file);
	}

	private static Collection<CodeLib2Element> readVer4(FileInputStream in) throws Exception {
		AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress((entry, ein) -> result.set(Serializer.BUILD_IN.deserialize(ein)), in);
		return result.get();
	}

	private static Collection<CodeLib2Element> readVer3(FileInputStream in) throws Exception {
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
