package multicast;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
	int id = 0;
	int tsid = -1;
	long timeStamp = 0;
	List<Integer> incomingNodes = new ArrayList<Integer>();
	public Vertex(int id, int tsid, long timeStamp) {
		this.id = id;
		this.tsid = tsid;
		this.timeStamp = timeStamp;
	}
	
}
