/** DataCruncherReadRowVarColTest.java
 * 
 * Regression test for DataCruncher.readRowVarCol().
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;


/**
 * @author kampe
 *
 */
public class DataCruncherReadRowVarColTest {

	@Test
	public void testReadRowVarCol() {
		String file = "testdata" + File.separator + "us50-short.tagged";
		try {
			BufferedReader tin = new BufferedReader(new FileReader(file));
			int numLabels = 7;
			int[] t = new int[7];
			String[][] cArray = new String[7][0];
			String tagDelimit = "|";
			String delimit = ",\t/ -():.;'?#`&\"_";
			String impDelimit = ",";
			int ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray);
			assertEquals(ptr, 4);
			assertEquals(t[3], 7);
			assertEquals(cArray[1][1], ",");
			assertEquals(cArray[0][2], "road");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

  @Test
  public void testReadRowVarColWithoutDowncasing() {
    String file = "testdata" + File.separator + "us50-short.tagged";
    try {
      BufferedReader tin = new BufferedReader(new FileReader(file));
      int numLabels = 7;
      int[] t = new int[7];
      String[][] cArray = new String[7][0];
      String tagDelimit = "|";
      String delimit = ",\t/ -():.;'?#`&\"_";
      String impDelimit = ",";
      int ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray,false);
      assertEquals(ptr, 4);
      assertEquals(t[3], 7);
      assertEquals(cArray[1][1], ",");
      assertEquals(cArray[0][2], "road");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
	public void testReadRowVarColEof() {
		String file = "testdata" + File.separator + "us50-short.tagged";
		try {
			BufferedReader tin = new BufferedReader(new FileReader(file));
			int numLabels = 7;
			int[] t = new int[7];
			String[][] cArray = new String[7][0];
			String tagDelimit = "|";
			String delimit = ",\t/ -():.;'?#`&\"_";
			String impDelimit = ",";
			int ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray);
			//Will run into end of file
			ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray);
			assertEquals(ptr, 4);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
  @Test
  public void testReadRowVarColEofWithoutDowncasing() {
    String file = "testdata" + File.separator + "us50-short.tagged";
    try {
      BufferedReader tin = new BufferedReader(new FileReader(file));
      int numLabels = 7;
      int[] t = new int[7];
      String[][] cArray = new String[7][0];
      String tagDelimit = "|";
      String delimit = ",\t/ -():.;'?#`&\"_";
      String impDelimit = ",";
      int ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray,false);
      //Will run into end of file
      ptr = DataCruncher.readRowVarCol(numLabels, tin, tagDelimit, delimit,impDelimit,t,cArray,false);
      assertEquals(ptr, 4);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
