
package mysh.codelib2.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

@Ignore
public class DataHeaderTest {

	private Collection<CodeLib2Element> eles;

	@Before
	public void prepare() {

		Random r = new Random();

		int len = 10000;
		this.eles = new ArrayList<>();

		CodeLib2Element e;
		for (int i = 0; i < len; i++) {
			e = new CodeLib2Element().setKeywords(Long.toString(r.nextLong()));
			e.setContent(new byte[10000]);
			// r.nextBytes(e.getContent());
			eles.add(e);
		}
	}

	@Test
	public void compressSaveReadTest() throws Exception {

		DataHeader dataHeader = new DataHeader();
		this.saveReadTest(dataHeader);
	}

	@Test
	public void unCompressSaveReadTest() throws Exception {

		DataHeader dataHeader = new DataHeader();
		dataHeader.setCompressed(false);
		this.saveReadTest(dataHeader);
	}

	private void saveReadTest(DataHeader dataHeader) throws Exception {

		String filepath = "e:/test/test.zcl2";
		if (dataHeader.isCompressed())
			filepath += ".comp";

		long start = System.nanoTime();
		dataHeader.saveToFile(new File(filepath), this.eles);
		System.out.println("save cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");

		start = System.nanoTime();
		Collection<CodeLib2Element> readEles = DataHeader.readFromFile(filepath);
		System.out.println("read cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");

		start = System.nanoTime();
		Assert.assertEquals(this.eles, readEles);
		System.out.println("check cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");
	}

}
