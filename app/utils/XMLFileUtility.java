package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.thoughtworks.xstream.XStream;

public class XMLFileUtility {
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy-H:m:s");

	public static void createXMLFile(Object object, String fileName) {
		fileName = "xmls/" + fileName.split("\\.")[0] + dateFormat.format(new Date()) + ".xml";
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
		filename =  "xmls/" + filename.split("\\.")[0] + dateFormat.format(new Date()) + ".xml";
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
