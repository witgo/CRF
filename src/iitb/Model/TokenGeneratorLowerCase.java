/*
 * Created on May 4, 2008
 * @author sunita
 */
package iitb.Model;

public class TokenGeneratorLowerCase extends TokenGenerator {
    @Override
    public Object getKey(Object xArg) {
        return xArg.toString().toLowerCase();
    }
}
