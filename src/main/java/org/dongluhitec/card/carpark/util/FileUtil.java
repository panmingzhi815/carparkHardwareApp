package org.dongluhitec.card.carpark.util;

import com.google.common.io.Closer;

import java.io.*;

/**
 * Created by panmingzhi815 on 2015/10/9 0009.
 */
public class FileUtil {

    public static void writeObjectToFile(Object obj,String filePath) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(filePath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);){
            oos.writeObject(obj);
        }catch(Exception e){
            throw new IOException("д���������ʧ��:"+filePath,e);
        }
    }

    public static Object readObjectFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()){
            return null;
        }
        try(FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis)){
            return ois.readObject();
        }catch(Exception e){
            throw new IOException("��ȡ��������ʧ��:"+filePath,e);
        }
    }

}
