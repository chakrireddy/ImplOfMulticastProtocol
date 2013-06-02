package multicast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

/*
 */
public class Reader implements Runnable{

	int count;
	Multicast multicast = Multicast.getInstance();
	File inputFile = null;
	//private BufferedReader readFile = null;
	/** Creates a new instance of Reader */
	public Reader() {

		try {
			String pathname = "input_"+multicast.myNode.id;
			inputFile = new File(pathname);
			if(!inputFile.exists()){
				inputFile.createNewFile();
			}
			count = 0;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			//System.out.println(e + "in InitReader");
		}
	}
	
	void printTopology(){
		for (int key : multicast.topology.keySet()) {
			List<Integer> lst =  multicast.topology.get(key);
			for (Integer integer : lst) {
				System.out.println(key+" "+integer);
			}
		}
	}

	@Override
	public void run() {
		FileInputStream is = null;
		try {
			is = new FileInputStream(inputFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		FileChannel channel = is.getChannel();
		FileLock lock = null;
		
		while(true){
			try {				
				int temp = 0;
				String msg = null;
				while((lock = channel.tryLock(0L, Long.MAX_VALUE, true))==null){
					
				}
				BufferedReader readFile = new BufferedReader(new InputStreamReader(is));
				//readFile = new BufferedReader(new FileReader(inputFile));
				while ((msg = readFile.readLine()) != null) {
					++temp;
					if (temp > count) /* new msg */
					{
						//System.out.println("Message: "+msg);
						if(msg.startsWith("hello")){
							//hello ID
							System.out.println("Message: "+msg);
							int id = Integer.parseInt(msg.split("\\s+")[1]);
							if(!multicast.incommingNeigh.contains(msg)){
								multicast.incommingNeigh.add(id);
							}
						}else if(msg.startsWith("linkstate")){
							
							multicast.linkmutex.acquire();
							//linkstate ID TS n-ID n-ID
							String[] arr = msg.split("\\s+");
							int id = Integer.parseInt(arr[1]);
							int ts = Integer.parseInt(arr[2]);
							if(multicast.timeStamps.get(id)==null){
								Vertex tss = new Vertex(id, -1, System.currentTimeMillis());
								multicast.timeStamps.put(id,tss);
							}
						if(multicast.timeStamps.get(id)!=null && multicast.timeStamps.get(id).tsid<ts){
							System.out.println("Message: "+msg);
							multicast.timeStamps.get(id).tsid = ts;
							multicast.timeStamps.get(id).incomingNodes.clear();
							for (int i = 3; i < arr.length; i++) {
								//node id reachable form arr[i]
								List<Integer> connected = multicast.topology.get(Integer.parseInt(arr[i]));
								if(connected == null){
									//create new list and add element to array.
									connected = new ArrayList<Integer>();
									connected.add(id);
									multicast.topology.put(Integer.parseInt(arr[i]), connected);
									
									//multicast.timeStamps.put(id,ts);
								}else {
									//get the list and update it.
									//multicast.timeStamps.put(id,-1);
									connected.add(id);
								}
								multicast.timeStamps.get(id).incomingNodes.add(Integer.parseInt(arr[i]));
							}
							multicast.writer.writeFile(msg);
							printTopology();
						}
						multicast.linkmutex.release();	
						}else if(msg.startsWith("join")){
							//join ID SID PID id0 id1 id2 ... idn
							System.out.println("Message: "+msg);
							String[] joinArr = msg.split("\\s+");
							if(joinArr.length==4 && joinArr[3].equals(Integer.toString(multicast.myNode.id))){
								//keep the info
								multicast.multicastSid = Integer.parseInt(joinArr[2]);
								multicast.multiChild.add(Integer.parseInt(joinArr[1]));
								//calculate the shortest path to src.
								//calculate the shortest path to send msg to its parent.
								//////////////////////
								try {
									multicast.linkmutex.acquire();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								//compute shortest path to from src to receiver
								String shrtPath = multicast.computePath(multicast.multicastSid, multicast.myNode.id);
								//compute shortest path and make join message
								if(shrtPath != null && !shrtPath.equals("empty")){
									multicast.multicastPid = shrtPath.charAt(0);
									String tempPath = multicast.computePath(shrtPath.charAt(0), multicast.myNode.id);
									if(tempPath == null){
										//nopath
									}else if(tempPath.equals("empty")){
										//has direct path
										//join ID SID PID
										multicast.writer.writeFile("join "+multicast.myNode.id+" "+multicast.multicastSid+" "+shrtPath.charAt(0));
									}else{
										//has intermediate nodes
										//join ID SID PID id0 id1 id2 ... idn
										StringBuffer tempJoin = new StringBuffer("join "+multicast.myNode.id+" "+multicast.multicastSid+" "+shrtPath.charAt(0));
										for (int i = 0; i < tempPath.length(); i++) {
												tempJoin.append(" ");
												tempJoin.append(tempPath.charAt(i));
										}
										multicast.writer.writeFile(tempJoin.toString());
									}
								}				
								multicast.linkmutex.release();
								///////////////////////////
							}else if(joinArr.length>4 && joinArr[4].equals(Integer.toString(multicast.myNode.id))){
								//remove the this id in the msg and pass the message
								StringBuffer joinBuff = new StringBuffer(joinArr[0]);
								for (int i = 1; i < joinArr.length; i++) {
									if(i != 4){
										joinBuff.append(" ");
										joinBuff.append(joinArr[i]);
									}
								}
								multicast.writer.writeFile(joinBuff.toString());								
							}
						}else if(msg.startsWith("data")){
							System.out.println("Message: "+msg);
							//data sender-ID root-ID string
							//read and forward the data
							String[] msgArr = msg.split("\\s+");
							if(msgArr[1].equals(Integer.toString(multicast.multicastPid))&& msgArr[2].equals(Integer.toString(multicast.multicastSid))){
							//forward the message
								multicast.writer.writeFile(msgArr[0]+" "+multicast.myNode.id+" "+multicast.multicastSid+" "+msgArr[3]);
							}
						}
					}
				}
				count = temp;
				lock.release();
				Thread.sleep(100);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (Exception e) {
				e.printStackTrace();
				//System.out.println(e + " in readFile()");
			}			
		}
		
	}
}
