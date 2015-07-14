
package mysh.codelib2.model;

import mysh.util.Compresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 文件数据头部. <br/>
 * 描述数据基本信息(如是否压缩, 是否加密). <br/>
 *
 * @author Allen
 */
public class DataHeader implements Serializable {
	private static final long serialVersionUID = -5817161670435220173L;

	private static final Logger log = LoggerFactory.getLogger(DataHeader.class);
	private static transient final String compressEntry = "zcl2";

	/**
	 * 压缩标记.
	 */
	private volatile boolean compressed = true;

	// /**
	// * 加密标记.
	// */
	// private volatile boolean encrypted = false;

	/**
	 * 数据保存到文件.<br/>
	 *
	 * @param filepath 文件名.
	 * @param eles     数据集.
	 */
	public void saveToFile(String filepath, Collection<CodeLib2Element> eles) throws Exception {

		ObjectOutputStream codeDataSerialOut;
		try (final FileOutputStream fileOut = new FileOutputStream(filepath)) {

			// write header to file
			ObjectOutputStream fileObjOut = new ObjectOutputStream(fileOut);
			fileObjOut.writeObject(this);
			fileObjOut.flush();

			codeDataSerialOut = fileObjOut;
			ExecutorService exec = Executors.newCachedThreadPool();

			// need Compress?
			Callable<Boolean> compressThread = null;
			Future<Boolean> compressFutureResult = null;
			if (this.compressed) {
				PipedOutputStream serialDataOut = new PipedOutputStream();
				final PipedInputStream compressIn = new PipedInputStream(serialDataOut,
								Compresses.DEFAULT_BUF_SIZE);
				codeDataSerialOut = new ObjectOutputStream(serialDataOut);

				// compress thread
				compressThread = new Callable<Boolean>() {

					private OutputStream compressOut = fileOut;

					@Override
					public Boolean call() throws Exception {

						return Compresses.compress(DataHeader.compressEntry,
								compressIn, Long.MAX_VALUE,
								this.compressOut, 0);
					}
				};

				compressFutureResult = exec.submit(compressThread);
				exec.shutdown();
			}

			// write codeData obj, use try-with to ensure objOutput close
			try (ObjectOutputStream tempCodeDataSerialOut = codeDataSerialOut) {
				tempCodeDataSerialOut.writeObject(eles);
			}

			if (this.compressed && compressThread != null) {
				boolean compressResult = compressFutureResult.get();
				if (!compressResult)
					throw new Exception("数据压缩失败.");
			}

		} catch (Exception e) {
			log.error("保存库文件失败.", e);
			throw e;
		}
	}

	/**
	 * 从文件读取数据.
	 *
	 * @param filepath 文件路径.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Collection<CodeLib2Element> readFromFile(String filepath) throws Exception {

		try (final FileInputStream fileIn = new FileInputStream(filepath)) {

			ObjectInputStream headerInput = new ObjectInputStream(fileIn);
			DataHeader header = (DataHeader) headerInput.readObject();

			if (header == null)
				throw new Exception("读取文件头失败.");

			if (header.compressed) {
				final List<Collection<CodeLib2Element>> result = new ArrayList<>();
				boolean deCompressResult = Compresses.deCompress((entry, in) -> {

					try {
						ObjectInputStream objIn = new ObjectInputStream(in);
						if (DataHeader.compressEntry.equals(entry.getName())) {
							result.add((Collection<CodeLib2Element>) objIn.readObject());
						}
					} catch (Exception e) {
						throw new RuntimeException("解压失败: " + e);
					}
				}, fileIn);

				if (deCompressResult) {
					return result.get(0);
				} else {
					throw new Exception("解压失败.");
				}
			} else {
				return (Collection<CodeLib2Element>) headerInput.readObject();
			}
		} catch (Exception e) {
			log.error("打开库文件失败.", e);
			throw e;
		}
	}

	/**
	 * 取压缩标记.
	 */
	public boolean isCompressed() {

		return compressed;
	}

	/**
	 * 设置压缩标记.
	 */
	public void setCompressed(boolean compressed) {

		this.compressed = compressed;
	}

	// /**
	// * 取加密标记.
	// *
	// * @return
	// */
	// public boolean isEncrypted() {
	//
	// return encrypted;
	// }
	//
	// /**
	// * 设置加密标记.
	// *
	// * @param encrypted
	// */
	// public void setEncrypted(boolean encrypted) {
	//
	// this.encrypted = encrypted;
	// }

}
