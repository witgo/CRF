package iitb.Segment;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class DataCruncherCreateRawTest {

	@Test
	public void testCreateRaw() {
		String file = "testdata" + File.separator + "us50-short";
		DataCruncher.createRaw(file, "|");
		File raw = new File("testdata" + File.separator + "us50-short.raw");
		File result = new File("testdata" + File.separator + "us50-short.result");
		raw.deleteOnExit();
		String rawText = "something";
		String resultText = "something else";
		try {
			rawText = readFile(raw);
			resultText = readFile(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(resultText, rawText);
	}
	
	public String readFile(File file) throws IOException {
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append("\n");
		}
		return buffer.toString();
	}
}
