/** LabelMapTest.java
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
public class LabelMapTest {

	@Test
	public void testMapLabelMap() {
		LabelMap map = new LabelMap();
		int value = map.map(5);
		assertEquals(4, value);
	}
	
	@Test
	public void testRevMapLabelMap() {
		LabelMap map = new LabelMap();
		int value = map.revMap(-1);
		assertEquals(0, value);
	}
	
	@Test
	public void testMapBinaryLabelMap() {
		BinaryLabelMap map = new BinaryLabelMap(-7);
		int value = map.map(7);
		assertEquals(0, value);
	}
	
	@Test
	public void testMapBinaryLabelMapEquals() {
		BinaryLabelMap map = new BinaryLabelMap(-7);
		int value = map.map(-7);
		assertEquals(1, value);
	}
	
	@Test
	public void testRevMapBinaryLabelMapOne() {
		BinaryLabelMap map = new BinaryLabelMap(-7);
		int value = map.revMap(1);
		assertEquals(-7, value);
	}
	
	@Test
	public void testRevMapBinaryLabelMapEquals() {
		BinaryLabelMap map = new BinaryLabelMap(-7);
		int value = map.revMap(0);
		assertEquals(0, value);
	}
	
	@Test
	public void testRevMapBinaryLabelMapNonBinary() {
		BinaryLabelMap map = new BinaryLabelMap(-7);
		int value = map.revMap(2);
		assertEquals(0, value);
	}
}
