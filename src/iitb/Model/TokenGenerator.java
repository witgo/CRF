package iitb.Model;

import java.io.Serializable;

public class TokenGenerator implements Serializable {
    boolean firstCall;
    Object x;
    public void startScan(Object xArg) {x=xArg; firstCall=true;}
    public boolean hasNext() {return firstCall;}
    public Object next() {firstCall = false; return x;}
    public Object getKey(Object xArg) {return xArg; }
};
