
package mysh.codelib2.model;

import mysh.collect.Pair;
import mysh.util.Compresses;
import mysh.util.FilesUtil;
import mysh.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
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
		out.write(Serializer.buildIn.serialize(this));
		out.flush();
	}

	private static DataHeader readHeader(InputStream in) {
		return Serializer.buildIn.deserialize(in);
	}

	private boolean writeVer4(File file, Collection<CodeLib2Element> eles) throws Exception {
		file.getParentFile().mkdirs();
		File writeFile = FilesUtil.getWriteFile(file);
		try (FileOutputStream out = new FileOutputStream(writeFile)) {
			writeHeader(out);
			Compresses.compress("zcl", new ByteArrayInputStream(Serializer.buildIn.serialize((Serializable) eles)),
							Long.MAX_VALUE, out, 500_000);
		}
		file.delete();
		return writeFile.renameTo(file);
	}

	private static Collection<CodeLib2Element> readVer4(FileInputStream in) throws Exception {
		AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress((entry, ein) -> result.set(Serializer.buildIn.deserialize(ein)), in);
		return result.get();
	}

	private boolean writeVer3(File file, Collection<CodeLib2Element> eles) throws Exception {
		file.getParentFile().mkdirs();
		File writeFile = FilesUtil.getWriteFile(file);
		try (FileOutputStream out = new FileOutputStream(writeFile)) {
			writeHeader(out);
			Compresses.compress("zcl", new ByteArrayInputStream(Serializer.fst.serialize((Serializable) eles)),
							Long.MAX_VALUE, out, 500_000);
		}
		file.delete();
		return writeFile.renameTo(file);
	}

	private static Collection<CodeLib2Element> readVer3(FileInputStream in) throws Exception {
		AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		Compresses.deCompress((entry, ein) -> result.set(Serializer.fst.deserialize(ein)), in);
		return result.get();
	}

	private boolean writeVer2(File file, Collection<CodeLib2Element> eles) throws Exception {
		try (final FileOutputStream fileOut = new FileOutputStream(file)) {
			writeHeader(fileOut);

			PipedOutputStream serialDataOut = new PipedOutputStream();
			final PipedInputStream compressIn = new PipedInputStream(serialDataOut, 100_000);
			ObjectOutputStream codeDataSerialOut = new ObjectOutputStream(serialDataOut);

			// compress thread
			Future<Boolean> compressFutureResult = ForkJoinPool.commonPool().submit(
							() -> Compresses.compress("zcl", compressIn, Long.MAX_VALUE, fileOut, 0));

			// write codeData obj, use try-with to ensure objOutput close
			codeDataSerialOut.writeObject(eles);
			codeDataSerialOut.close();

			boolean compressResult = compressFutureResult.get();
			if (!compressResult)
				throw new Exception("数据压缩失败.");
			return true;
		} catch (Exception e) {
			log.error("保存库文件失败.", e);
			throw e;
		}
	}

	private static Collection<CodeLib2Element> readVer2(InputStream in) throws Exception {
		final AtomicReference<Collection<CodeLib2Element>> result = new AtomicReference<>();
		boolean deCompressResult = Compresses.deCompress(
						(entry, ein) -> {
							try {
								ObjectInputStream objIn = new ObjectInputStream(ein);
								result.set((Collection<CodeLib2Element>) objIn.readObject());
							} catch (Exception e) {
								throw new RuntimeException("解压失败: " + e);
							}
						}, in);

		if (deCompressResult) {
			return result.get();
		} else {
			throw new Exception("解压失败.");
		}
	}

}
