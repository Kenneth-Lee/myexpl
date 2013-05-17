package app.myexpl;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class FileCipher {

    @SuppressWarnings("unused")
	private final static String alg = "AES";

    /**
     * Mask a file with password hash for <i>len</i> bytes.
     * 
     * @param password the raw password for hash
     * @param file the file to masked
     * @param len how many bytes to mask, >0
     * @throws Exception when fail
     */
    public static void mask(String password, File file, int len) throws Exception {
        if(password==null || password.length()==0) 
            throw new Exception("empty password");

        if(len<=0) 
            throw new Exception("len <= 0");
        else if(file.length()<len) {
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
    
    public interface MaskProgressListener {
    	public void progressUpdate(int progress);
    }

    public static int maskStep = 4096;
    
    public static void fullMask(String password, File file, MaskProgressListener listener) throws Exception {
    	long flen = file.length();
    	int len=0;
    	byte[] rkey = MyCipher.getRawKey(password);
    	
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        byte[] buf = new byte[maskStep];
        
        int pos = 0;
        while(pos<flen) {
            f.seek(pos);
        	len = f.read(buf, 0, maskStep);
        	MyCipher.mask(rkey, buf, len);
        	f.seek(pos);
            f.write(buf, 0, len);
        	pos+=len;
        	listener.progressUpdate((int)(pos*100/flen));
        }
        f.close();
    }
}

/* vim: set et ts=4 sw=4: */
