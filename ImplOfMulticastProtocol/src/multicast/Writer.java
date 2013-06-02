package multicast;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;

public class Writer {

	int count;
	Multicast multicast = Multicast.getInstance();
	File outputFile = null;
	//BufferedWriter bufferedWriter = null;
	FileChannel fileChannel = null;
	public Writer(String path) {

		try {
			String pathname = path;
			outputFile = new File(pathname);
			/*bufferedWriter = new BufferedWriter(
					new FileWriter(outputFile, true));*/
			 fileChannel = new RandomAccessFile(outputFile, "rw").getChannel();
			count = 0;
			
		} catch (Exception e) {
			e.printStackTrace();
			//System.out.println(e + "in writer");
		}
	}

	synchronized void writeFile(String msg) {
		try {
			FileLock lock = null;
			while((lock = fileChannel.lock())==null){
				Thread.sleep(10);
			}
			fileChannel.write(Charset.defaultCharset().encode(msg+"\n"));
			//bufferedWriter.write(msg+"\n");
			//bufferedWriter.close();
			count++;
			lock.release();
		} catch (Exception e) {
			e.printStackTrace();
			//System.out.println(e + " in writeFile()");
		}
	}
}
