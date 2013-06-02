package multicast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {
	int count;
	Multicast multicast = Multicast.getInstance();
	
	public Controller() {
		Thread fileFinder = new Thread(new FileFinder());
		fileFinder.start();
	}
}

class FileFinder implements Runnable {

	Map<String, File> outputFiles = new HashMap<String, File>();

	@Override
	public void run() {
		while (true) {
			File folder = new File(".");
			File[] fileArr = folder.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("output");
				}
			});
			for (File file : fileArr) {
				//System.out.println("fileName: " + file.getName());
				File opfile = outputFiles.get(file.getName());
				if (opfile == null) {
					new Thread(new FileListener(file)).start();
					outputFiles.put(file.getName(), file);
				}
			}
		}
	}

	public void findNewMessage(File file) {

	}
}

class FileListener implements Runnable {
	File file = null;
	int count = 0;
	Multicast multi = Multicast.getInstance();
	//ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	public FileListener(File file) {
		this.file = file;
	}

	@Override
	public void run() {
		while (true) {
			try {
				int temp = 0;
				String msg = null;
				//lock.readLock().lock();
				//FileReader fr = new FileReader(file);
				FileInputStream is = new FileInputStream(file);
				FileChannel channel = is.getChannel();
				FileLock lock = null;
				while((lock = channel.tryLock(0L, Long.MAX_VALUE, true))==null){
					
				}
				BufferedReader readFile = new BufferedReader(new InputStreamReader(is));
				while ((msg = readFile.readLine()) != null) {
					++temp;
					if (temp > count) /* new msg */
					{
						System.out.println("Message: " + msg);
						List<Integer> connected = multi.topology.get(Integer.parseInt(file.getName().split("_")[1]));
						// TODO parse message and do something
						for (Integer integer : connected) {
							String filePath = "input_"+integer;
							//"input_" + multicast.myNode.id
							//System.out.println("path: "+filePath);
							BufferedWriter WriteFile = new BufferedWriter( new	FileWriter(filePath, true)); 
							WriteFile.write(msg+"\n");
							//WriteFile.write("\n");
							WriteFile.close();
						}						
					}
				}
				count = temp;
				//lock.readLock().unlock();
				lock.release();
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
				//System.out.println(e + " in readFile()");
			}
		}
	}
}