/*
 * Created on Dec 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package iitb.CRF;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class RestrictConstraint extends Constraint {
	/**
	 * @return
	 */
	public abstract int numAllowed();
	public abstract int allowed(int c);
	public final int type() {return ALLOW_ONLY;}	
}
