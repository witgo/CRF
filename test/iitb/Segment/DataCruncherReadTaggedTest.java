/** DataCruncherReadTaggedTest.java
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import iitb.CRF.DataSequence;

import java.io.File;

import org.junit.Test;


/**
 * @author kampe
 *
 */
public class DataCruncherReadTaggedTest {

	@Test
	public void testReadTagged() {
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
		assertTrue(data.hasNext());
		assertEquals(2, data.size());
		DataSequence seq = data.next();
		assertEquals(8, seq.length());
		assertEquals("road", seq.x(2));
		assertEquals(",", seq.x(3));	
	}

  @Test
  public void testReadTaggedWithoutLowerCasing() {
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
    TrainData data = DataCruncher.readTagged(numLabels, tfile, rfile, delimit, tagDelimit, impDelimit, labelMap,false);
    assertTrue(data.hasNext());
    assertEquals(2, data.size());
    DataSequence seq = data.next();
    assertEquals(8, seq.length());
    assertEquals("Road", seq.x(2));
    assertEquals(",", seq.x(3));
  }
}
