package iitb.Model;

import java.io.Serializable;

public class Edge implements Comparable, Serializable {
    int start;
    int end;
    Edge() {;}
    public Edge(int s, int e) {
	start = s;
	end = e;
    }
    String tostring() {
	return (start + " -> " + end);
    }
    public int compareTo(Object o) {
	Edge e = (Edge)o;
	return ((start != e.start)?(start - e.start):(end-e.end));
    }
};
