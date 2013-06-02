package multicast;

import java.io.Serializable;

public class Node implements Serializable{

	private static final long serialVersionUID = 1L;
	public int id = 0;
	public Node(int id) {
		this.id = id;
	}
}
