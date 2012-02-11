/** DataCruncherReadRowFixedColTest.java
 * 
 * Regression test for DataCruncher.readRowFixedCol().
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;


/**
 * @author kampe
 *
 */
public class DataCruncherReadRowFixedColTest {

	private static final String tagged = "fixed-column-format\n"
		+ "|3|5|6|7|\n"
		+ "Homer Spit Road,|Homer,|AK|99603\n"
		+ "Lnlck Shopping Center,|Anniston,|AL|36201";
	
	@Test
	public void testReadRowFixedCol() {
		int numLabels = 7;
		BufferedReader reader = new BufferedReader(new StringReader(tagged));
		String tagDelimit = "|";
		String delimit = ",\t/ -():.;'?#`&\"_";
		String impDelimit = ",";
		int[] t = new int[numLabels];
		String[][] cArray = new String[numLabels][0];
		try {
			int[] labels = DataCruncher.readHeaderInfo(numLabels, reader, tagDelimit);
			int ptr = DataCruncher.readRowFixedCol(numLabels, reader,
					tagDelimit, delimit, impDelimit, t, cArray, labels, null);
			assertEquals(ptr, 4);
			assertEquals(cArray[0][2], "road");
			assertEquals(cArray[3][0], "99603");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testReadRowFixedColSecond() {
		int numLabels = 7;
		BufferedReader reader = new BufferedReader(new StringReader(tagged));
		String tagDelimit = "|";
		String delimit = ",\t/ -():.;'?#`&\"_";
		String impDelimit = ",";
		int[] t = new int[numLabels];
		String[][] cArray = new String[numLabels][0];
		try {
			int[] labels = DataCruncher.readHeaderInfo(numLabels, reader, tagDelimit);
			int ptr = DataCruncher.readRowFixedCol(numLabels, reader,
					tagDelimit, delimit, impDelimit, t, cArray, labels, null);
			t = new int[numLabels];
			cArray = new String[numLabels][0];
			ptr = DataCruncher.readRowFixedCol(numLabels, reader,
					tagDelimit, delimit, impDelimit, t, cArray, labels, null);
			assertEquals(ptr, 4);
			assertEquals(cArray[0][2], "center");
			assertEquals(cArray[3][0], "36201");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
