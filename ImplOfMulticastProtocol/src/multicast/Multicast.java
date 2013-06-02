package multicast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Multicast {
	Logger logger = Logger.getLogger(Multicast.class.getName());
	public FileHandler fileHandler = null;
	private static Multicast multicast = null;
	Node myNode = null;
	String[] args = null;
	Semaphore mutex = new Semaphore(1);
	Semaphore linkmutex = new Semaphore(1);
	boolean controller = false;
	Map<Integer,List<Integer>> topology = new HashMap<Integer,List<Integer>>();
	Set<Integer> incommingNeigh = new HashSet<Integer>();
	int TS = 0;
	int readCount = 0;
	File inputFile = null;
	boolean sender = false;
	boolean receiver = false;
	Writer writer = null;
	String path = null;
	Map<Integer,Vertex> timeStamps = new HashMap<Integer, Vertex>();
	int multicastSid = 0;
	List<Integer> multiChild = new ArrayList<Integer>();
	int multicastPid = 0;
	private Multicast() {
		
	}
	
	public synchronized static Multicast getInstance(){
		if(multicast == null){
			multicast = new Multicast();
		}
		return multicast;
	}
	
	public static void main(String[] args) {
		Multicast multi = Multicast.getInstance();
		multi.args = args;
		multi.init();
		multi.run();
		
	}
	
	public void init(){
		File logfile = new File("Log.txt");
		Formatter format = new SimpleFormatter();
		try {
			fileHandler = new FileHandler(logfile.getAbsolutePath());
			fileHandler.setFormatter(format);
			logger.addHandler(fileHandler);
		} catch (SecurityException e1) {
			logger.log(Level.INFO, e1.getMessage());
			e1.printStackTrace();
		} catch (IOException e1) {
			logger.log(Level.INFO, e1.getMessage());
			e1.printStackTrace();
		}
		String id = null;
		try {
			id = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		if(id != null){
			String srcId = null;
			String sendMsg = null;
			myNode = new Node(Integer.parseInt(args[0]));
			try {
				if(sender = args[1].equals("sender")){
					 sendMsg = args[2];
				}else if(receiver= args[1].equals("receiver")){
					srcId = args[2];
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();				
			}
			writer = new Writer("output_" + multicast.myNode.id);
			/*for (int i = 0; i < 10; i++) {
				writer.writeFile("hello "+multicast.myNode.id);
			}*/
			HelloThread hellothrd = new HelloThread(writer);
			LinkStateThread linkthrd = new LinkStateThread(writer);
			Thread hello = new Thread(hellothrd);
			hello.start();
			Thread link = new Thread(linkthrd);
			link.start();
			Thread readerThread = new Thread(new Reader());
			readerThread.start();
			/*try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			while(topology.get(myNode.id)==null||(topology.get(myNode.id)!=null) && topology.get(myNode.id).size()==0){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(receiver){
				System.out.println("receiver ****");
				boolean pathFound = false;
				while(!pathFound){
				try {
					linkmutex.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("acquired lock");
				//compute shortest path to from src to receiver
				String shrtPath = computePath(Integer.parseInt(srcId), myNode.id);
				//compute shortest path and make join message
				if(shrtPath != null){
					String tempPath = computePath(shrtPath.charAt(0), myNode.id);
					System.out.println("temp path: "+tempPath);
					if(tempPath == null){
						//nopath
						System.out.println("no path");
					}else if(tempPath.equals("empty")){
						pathFound = true;
						//has direct path
						//join ID SID PID
						multicastPid = shrtPath.charAt(0);
						logger.info("join command");
						writer.writeFile("join "+myNode.id+" "+Integer.parseInt(srcId)+" "+multicastPid);
					}else{
						logger.info("join commands");
						pathFound = true;
						//has intermediate nodes
						//join ID SID PID id0 id1 id2 ... idn
						multicastPid = shrtPath.charAt(0);
						StringBuffer tempJoin = new StringBuffer("join "+myNode.id+" "+Integer.parseInt(srcId)+" "+multicastPid);
						for (int i = 0; i < tempPath.length(); i++) {
								tempJoin.append(" ");
								tempJoin.append(tempPath.charAt(i));
						}
						writer.writeFile(tempJoin.toString());
					}
				}				
				linkmutex.release();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				}
			}else if(sender){
				DataSender dataSender = new DataSender(this,sendMsg);
				Thread dataSndThr = new Thread(dataSender);
				dataSndThr.start();
			}
			
		}else {
			controller = true;
			loadConfig();
			Controller contlr = new Controller();
		}
		
	}
	
	public void loadConfig(){
		//read topology file
		try {
		BufferedReader buffFileReader = new BufferedReader(new FileReader("topology"));
		String line = null;		
			while ((line = buffFileReader.readLine()) != null) {
				String[] nodeInfo = line.split("\\s+");
				List<Integer> connected = topology.get(Integer.parseInt(nodeInfo[0]));
				if(connected == null){
					//create new list and add element to array.
					connected = new ArrayList<Integer>();
					connected.add(Integer.parseInt(nodeInfo[1]));
					topology.put(Integer.parseInt(nodeInfo[0]), connected);
					
				}else {
					//get the list and update it.
					connected.add(Integer.parseInt(nodeInfo[1]));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run(){
		
	}
	
	public String computePath(int src, int dest){
		int depth = 1;
		while(!shortestPath(src, dest,depth) && topology.size()> depth){
			depth++;
			path = null;
		}
		//System.out.println("path: "+path);
		if(depth == 1){
			//return empty set
			//System.out.println("empty");
			return "empty";
		}else if(topology.size()<= depth){
			//nopath
			//System.out.println("no path");
			return null;
		}else if(depth >1){
			//System.out.println("updated path: "+path.substring(0, path.length()-1));
			return new String(path.substring(0, path.length()-1));
		}
		return null;
	}
	
	public boolean shortestPath(int src, int dest,int depth){
		boolean found = false;
		if(depth == 1){
			//System.out.println("source: "+src+" "+topology.size());
			List<Integer> lst = topology.get(src);
			if(lst==null){
				return false;
			}
			for (Integer integer : lst) {
				if(integer == dest){
					path=Integer.toString(src);
					return true;
				}
			}
		}else  if(depth > 0){
			depth--;
			if(topology.get(src) == null){
				return false;
			}
			for (int index : topology.get(src)) {
				if(shortestPath(index, dest, depth)){
					path+=Integer.toString(src);
					return true;
				}
			}
		}
		return found;
	}
	
	int getWeight(int v, int i){
		return 1;
	}
	int[] getNeighbours(int id){
		Object[] arr = timeStamps.get(id).incomingNodes.toArray();
		int[] intArr = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			intArr[i] = (Integer)arr[i];
		}
		return intArr;
	}
	
	int size(){
		return timeStamps.size();
	}
	
	String getLabel(int id){
		return Integer.toString(id);
	}
}

class HelloThread implements Runnable{
	Writer writer = null;
	Multicast multi = Multicast.getInstance();
	public HelloThread(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void run() {
		while(true){
			writer.writeFile("hello "+multi.myNode.id);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}	
}

class LinkStateThread implements Runnable{
	Writer writer = null;
	Multicast multi = Multicast.getInstance();
	public LinkStateThread(Writer writer) {
		this.writer = writer;
	}
	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//TODO advertise link state.linkstate ID TS n-ID n-ID
			StringBuffer buff = new StringBuffer();
			buff.append("linkstate "+multi.myNode.id+" ");
			if(multi.TS>9){
				buff.append(multi.TS);
			}else {
				buff.append("0"+multi.TS);
			}
			Object[] ndidArr = multi.incommingNeigh.toArray();
			for (Object ndid : ndidArr) {
				buff.append(" ");
				buff.append(ndid);
			}
			if(ndidArr.length>0){
				writer.writeFile(buff.toString());
				multi.TS++;
			}
						
		}
	}
}

class DataSender implements Runnable{

	Multicast multicast = null;
	String data = null;
	public DataSender(Multicast multicast, String data) {
		this.multicast = multicast;
		this.data = data;
	}
	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			multicast.writer.writeFile("data "+multicast.myNode.id+" "+multicast.myNode.id+" "+data);
		}
	}
	
}

