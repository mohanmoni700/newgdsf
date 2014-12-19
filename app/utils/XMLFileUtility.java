package utils;

import com.thoughtworks.xstream.XStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by user on 14-11-2014.
 */
public class XMLFileUtility {

	public static void createXMLFile(Object object, String fileName) {
		Writer writer = null;
		XStream xStream = new XStream();
		try {
			String xml = xStream.toXML(object);
			writer = new FileWriter(fileName);
			writer.write(xml);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createFile(String content, String filename) {
		try {
			FileWriter fileWriter = new FileWriter(new File(filename));
			BufferedWriter writer = new BufferedWriter(fileWriter);
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
