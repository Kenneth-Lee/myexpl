package app.myexpl;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class FileCipher {

    @SuppressWarnings("unused")
	private final static String alg = "AES";

    public static void mask(String password, File file, int len) throws Exception {
        if(password==null || password.length()==0) 
            throw new Exception("empty password");

        if(len<=0) 
            throw new Exception("mask len="+len);

        if(file.length()<len) {
            len=(int)file.length();
        }

        RandomAccessFile f = new RandomAccessFile(file, "rw");
        FileChannel fc = f.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(len);
        fc.read(buf);
        MyCipher.mask(password, buf.array());
        buf.clear();
        fc.position(0L);
        fc.write(buf);
        fc.close();
        f.close();
    }
}

/* vim: set et ts=4 sw=4: */
