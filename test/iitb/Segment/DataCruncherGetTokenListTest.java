/** DataCruncherGetTokenListTest.java
 * 
 * @author kampe
 * @since 1.3
 * @version 1.3
 */
package iitb.Segment;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author kampe
 *
 */
public class DataCruncherGetTokenListTest {

	@Test
	public void testGetTokenListWithDelimiter() {
		String tokenString = "South 1/2 Greenwood Avenue, |3";
		String delimit = ",\t/ -():.;'?#`&\"_";
		String impDelimit = ",";
		String[] tokens = DataCruncher.getTokenList(tokenString, delimit, impDelimit);
		
		assertEquals(tokens.length, 7);
		assertEquals(tokens[1], "1");
		assertEquals(tokens[5], ",");
	}
	
	@Test
	public void testGetTokenList() {
		String tokenString = "West Goldfield Avenue, |3";
		String delimit = ",\t/ -():.;'?#`&\"_";
		String impDelimit = ",";
		String[] tokens = DataCruncher.getTokenList(tokenString, delimit, impDelimit);

		assertEquals(tokens.length, 5);
		assertEquals(tokens[1], "goldfield");
		assertEquals(tokens[4], "|3");
	}
  @Test
  public void testGetTokenListWithoutLowerCasing() {
    String tokenString = "West Goldfield Avenue, |3";
    String delimit = ",\t/ -():.;'?#`&\"_";
    String impDelimit = ",";
    String[] tokens = DataCruncher.getTokenList(tokenString, delimit, impDelimit,false);

    assertEquals(tokens.length, 5);
    assertEquals(tokens[1], "Goldfield");
    assertEquals(tokens[4], "|3");
  }
}
