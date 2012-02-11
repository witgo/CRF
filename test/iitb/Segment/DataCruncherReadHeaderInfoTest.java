/** DataCruncherReadHeaderInfoTest.java
 * 
 * Regression test for DataCruncher.readHeaderInfo().
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

/**
 * @author kampe
 *
 */
public class DataCruncherReadHeaderInfoTest {
	
	private static final String tagged = "fixed-column-format\n"
		+ "|3|5|6|7|\n"
		+ "Homer Spit Road,|Homer,|AK|99603\n\n"
		+ "Lnlck Shopping Center,|Anniston,|AL|36201";

	@Test
	public void testReadHeaderInfo() {
		int numLabels = 7;
		BufferedReader reader = new BufferedReader(new StringReader(tagged));
		String tagDelimit = "|";
		try {
			int[] labels = DataCruncher.readHeaderInfo(numLabels, reader, tagDelimit);
			assertTrue("Array was null", labels != null);
			assertEquals(labels[3], 7);
			assertEquals(labels[4], 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
