package iitb.Model;

public class Edge implements Comparable {
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
