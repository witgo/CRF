/** AlphaNumericPreprocessorTest.java
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;


/**
 * @author kampe
 *
 */
public class AlphaNumericPreprocessorTest {

	@Test
	public void testPreprocessor() {
		String file = "testdata" + File.separator + "us50-short";
		DataCruncher.createRaw(file, "|");
		File raw = new File("testdata" + File.separator + "us50-short.raw");
		raw.deleteOnExit();
		
		int numLabels = 7;
		String tfile = file;
		String rfile = file;
		String tagDelimit = "|";
		String delimit = ",\t/ -():.;'?#`&\"_";
		String impDelimit = ",";
		LabelMap labelMap = new LabelMap();
		TrainData data = DataCruncher.readTagged(numLabels, tfile, rfile, delimit, tagDelimit, impDelimit, labelMap);
		TrainData prepData = AlphaNumericPreprocessor.preprocess(data, numLabels);

		assertTrue(prepData != null);
		prepData.startScan();
		assertTrue(prepData.hasNext());
		TrainRecord rec = prepData.nextRecord();
		assertEquals(4, rec.numSegments());
		String[] tokens = rec.tokens(3);
		assertEquals("DIGIT", tokens[0]);
	}
}
