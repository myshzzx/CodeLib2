
package mysh.codelib2.model;

import mysh.collect.Pair;
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

		DataHeader dataHeader = new DataHeader(3);
		this.saveReadTest(dataHeader);
	}

	private void saveReadTest(DataHeader dataHeader) throws Exception {

		String filepath = "e:/test/test.zcl2";
		filepath += ".comp";

		long start = System.nanoTime();
		File file = new File(filepath);
		dataHeader.saveToFile(file, this.eles);
		System.out.println("save cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");

		start = System.nanoTime();
		Pair<DataHeader, Collection<CodeLib2Element>> data = DataHeader.readFromFile(file);
		System.out.println("read cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");

		start = System.nanoTime();
		Assert.assertEquals(this.eles, data.getR());
		System.out.println("check cost: " + (System.nanoTime() - start) / 1_000_000 + " mm");
	}

}
