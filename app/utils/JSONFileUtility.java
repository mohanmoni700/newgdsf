package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by user on 11-09-2014.
 */
public class JSONFileUtility {

    public static void createJsonFile(Object object, String fileName){
        Writer writer = null;
        try {
            writer = new FileWriter(fileName);
            Gson gson = new GsonBuilder().create();
            gson.toJson(object, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
