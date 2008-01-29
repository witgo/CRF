/*
 * Created on Jan 29, 2008
 * @author sunita
 */
package iitb.Model;

import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

public class CompleteModelRestricted extends GenericModel {

    public CompleteModelRestricted(String spec, int numLabels) throws Exception {
        super(numLabels,0);
        StringTokenizer tokens = new StringTokenizer(spec,":");
        tokens.nextToken(); // this is the name of the model.
        HashSet<Edge> followEdges = new HashSet<Edge>();
        HashSet<Integer> followLabels = new HashSet<Integer>();
        while (tokens.hasMoreTokens()) {
            int parent = Integer.parseInt(tokens.nextToken());
            int child = Integer.parseInt(tokens.nextToken());
            followEdges.add(new Edge(parent,child));
            followLabels.add(child);
        }
        _edges = new Edge[numLabels*numLabels-followEdges.size()*(numLabels-2)];
        
        startStates = new int[numLabels-followEdges.size()];
        for (int i = 0, st = 0; i < numLabels; i++) {
            if (!followLabels.contains(i))
                startStates[st++] = i;
        }
        endStates = new int[numLabels];
        for (int i = 0; i < endStates.length; i++) {
            endStates[i] = i;
        }
        
        
        edgeStart = new int[numLabels];
        for (int i = 0, edgeNum=0; i < numLabels; i++) {
            edgeStart[i] = edgeNum;
            for (int j = 0; j < numLabels; j++) {
                Edge edge = new Edge(i,j);
                if (followLabels.contains(j) && (i != j) && !followEdges.contains(edge)) {
                    continue;
                }
                _edges[edgeNum++] = edge;
            }
        }
    }
}
