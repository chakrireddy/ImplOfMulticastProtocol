package message;

import java.io.Serializable;

import multicast.Node;

public class Info implements Serializable{

	private static final long serialVersionUID = 1L;	
	public Node node = null;
	public Info(Node node) {
		this.node = node;
	}
}
