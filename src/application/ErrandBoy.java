package application;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import javafx.scene.control.TextArea;

/**
 * This class provide utilities for WatSup system. Use this to:
 * + quickly test functions
 * + provide a public place to store data
 * + execute common functions
 * 
 * This class should not be instantiated. 
 * @author PineXmas
 *
 */
public abstract class ErrandBoy {
	
	static boolean isPrintStackTrace = false;
	
	public static TextArea txtOutput1 = null;
	static Object lockTxtOutput1 = new Object();
	
	public static void Test() {
		System.out.println("hello");
	}
	
	/**
	 * (SYNC) Print message to console
	 * @param msg message to print
	 */
	synchronized public static void print(String msg) {
//		synchronized (lockTxtOutput) {
//			if (txtOutput != null) {
//				txtOutput.appendText(msg);
//			} else {
//				System.out.print(msg);
//			}
//		}
		
		//run as normal
		System.out.print(msg);
	}
	
	/**
	 * (SYNC) Print message to console & end with newline
	 * @param msg message to print
	 */
	synchronized public static void println(String msg) {
		print(msg + "\n");
	}
	
	/**
	 * (SYNC) Print error to console together with a headline
	 * @param e the exception where error happens
	 * @param errHeadline the headline
	 */
	synchronized public static void printlnError(Exception e, String errHeadline) {
		print(errHeadline);
		
		//get error stack trace into string & print to console
		if (isPrintStackTrace) {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(byteStream);
			e.printStackTrace(printStream);
			println("");
			println(byteStream.toString());
		} else {
			println(" (" + e.toString() + ")");
		}
	}
	
	/**
	 * Convert the given array-list of bytes to array of primitive-bytes
	 * @param listBytes
	 * @return
	 */
	public static byte[] convertList2Array(ArrayList<Byte> listBytes) {
		byte[] arr = new byte[listBytes.size()];
		
		for (int i = 0; i < arr.length; i++) {
			arr[i] = listBytes.get(i);
		}
		
		return arr;
	}
}
