package app.myexpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Images;

@TargetApi(8)
public class Utils {

	private static final String fs = System.getProperty("file.separator");
	
    public static String fileSize(long s) {
        if(s>1024*1024*1024) {
            return s/(1024*1024*1024)+"GB";
        }else if(s>1024*1024) {
            return s/(1024*1024)+"MB";
        }else if(s>1024) {
            return s/1024+"KB";
        }else {
            return s+"B";
        }
    }
    
    private static int dirCompare(final File o1, final File o2) {
  	  if(o1.isDirectory()) {
		  if(!o2.isDirectory()) {
			  return -1;
		  }
	  }else {
		  if(o2.isDirectory()) {
			  return 1;
		  }
	  }
  	  return 0;
    }
    
    public static Comparator<File> genFileNameComparator(final boolean isDirFirst) {
    	return new Comparator<File>() {
          public int compare(final File o1, final File o2) {
        	  if(isDirFirst) {
        		  int r = dirCompare(o1, o2);
        		  if(r!=0) {
        			  return r;
        		  }
        	  }
              return o1.getName().compareTo(o2.getName());
          }
        };
    }
    
    public static Comparator<File> genFileTimeComparator(final boolean isDirFirst) {
    	return new Comparator<File>() {
          public int compare(final File o1, final File o2) {
        	  if(isDirFirst) {
        		  int r = dirCompare(o1, o2);
        		  if(r!=0) {
        			  return r;
        		  }
        	  }
        	  return (int)o1.lastModified() - (int)o2.lastModified();
          }
        };
    }
    
    /**
     * if oldName end with .ext, return a string without the subfix;
     * or add .ext to the oldName and return it.
     * 
     * @param oldName old name
     * @param ext new name without the "."
     * @return name name
     */
    public static String switchNameExt(String oldName, String ext) {
    	if(oldName.endsWith("."+ext)) {
    		return oldName.substring(0, oldName.length()-1-ext.length());
    	}else {
    		return oldName+"."+ext;
    	}
    }

    public static void openFileWithSpecApp(String name, Activity app, File f) {
		Intent i = new Intent(name);
		i.setDataAndType(Uri.fromFile(f), null);
		app.startActivity(i);
    }
    
    public static void openFileWithApp(Activity app, File f) {
		Intent i = new Intent(android.content.Intent.ACTION_VIEW);
		i.setDataAndType(Uri.fromFile(f), Utils.getMimeType(f.getName()));
		app.startActivity(i);
    }
    
    public static String getMimeType(String path) {
    	final String[][] MIME_MapTable={  
                //{后缀名， MIME类型}  
                {".3gp",    "video/3gpp"},  
                {".apk",    "application/vnd.android.package-archive"},  
                {".asf",    "video/x-ms-asf"},  
                {".avi",    "video/x-msvideo"},  
                {".bin",    "application/octet-stream"},  
                {".bmp",    "image/bmp"},  
                {".c",  "text/plain"},  
                {".class",  "application/octet-stream"},  
                {".conf",   "text/plain"},  
                {".cpp",    "text/plain"},  
                {".doc",    "application/msword"},  
                {".docx",   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},  
                {".xls",    "application/vnd.ms-excel"},   
                {".xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},  
                {".exe",    "application/octet-stream"},  
                {".gif",    "image/gif"},  
                {".gtar",   "application/x-gtar"},  
                {".gz", "application/x-gzip"},  
                {".h",  "text/plain"},  
                {".htm",    "text/html"},  
                {".html",   "text/html"},  
                {".jar",    "application/java-archive"},  
                {".java",   "text/plain"},  
                {".jpeg",   "image/jpeg"},  
                {".jpg",    "image/jpeg"},  
                {".js", "application/x-javascript"},  
                {".log",    "text/plain"},  
                {".m3u",    "audio/x-mpegurl"},  
                {".m4a",    "audio/mp4a-latm"},  
                {".m4b",    "audio/mp4a-latm"},  
                {".m4p",    "audio/mp4a-latm"},  
                {".m4u",    "video/vnd.mpegurl"},  
                {".m4v",    "video/x-m4v"},   
                {".mov",    "video/quicktime"},  
                {".mp2",    "audio/x-mpeg"},  
                {".mp3",    "audio/x-mpeg"},  
                {".mp4",    "video/mp4"},  
                {".mpc",    "application/vnd.mpohun.certificate"},        
                {".mpe",    "video/mpeg"},    
                {".mpeg",   "video/mpeg"},    
                {".mpg",    "video/mpeg"},    
                {".mpg4",   "video/mp4"},     
                {".mpga",   "audio/mpeg"},  
                {".msg",    "application/vnd.ms-outlook"},  
                {".ogg",    "audio/ogg"},  
                {".pdf",    "application/pdf"},  
                {".png",    "image/png"},  
                {".pps",    "application/vnd.ms-powerpoint"},  
                {".ppt",    "application/vnd.ms-powerpoint"},  
                {".pptx",   "application/vnd.openxmlformats-officedocument.presentationml.presentation"},  
                {".prop",   "text/plain"},  
                {".rc", "text/plain"},  
                {".rmvb",   "audio/x-pn-realaudio"},  
                {".rtf",    "application/rtf"},  
                {".sh", "text/plain"},  
                {".tar",    "application/x-tar"},     
                {".tgz",    "application/x-compressed"},   
                {".txt",    "text/plain"},  
                {".wav",    "audio/x-wav"},  
                {".wma",    "audio/x-ms-wma"},  
                {".wmv",    "audio/x-ms-wmv"},  
                {".wps",    "application/vnd.ms-works"},  
                {".xml",    "text/plain"},  
                {".z",  "application/x-compress"},  
                {".zip",    "application/x-zip-compressed"},  
                {"",        "*/*"}    
    	};
    	
        String type="*/*";  
        int dotIndex = path.lastIndexOf(".");  
        if(dotIndex < 0){  
            return type;  
        }
        
        String end=path.substring(dotIndex, path.length()).toLowerCase();  
        if(end=="")
        	return type;  
        
        for(int i=0;i<MIME_MapTable.length;i++){  
            if(end.equals(MIME_MapTable[i][0]))  
                type = MIME_MapTable[i][1];  
        }         
        return type;  
    }
    
    /**
     * load ThumbnailImage from a file
     * 
     * @param path file path
     * @param w expected weight
     * @param h expected height
     * @return the bitmap
     */
    public static Bitmap loadThumbnailImage(File file, int w, int h) {
    	String mimetype = getMimeType(file.getName());
    	String path = file.getAbsolutePath();
    	
    	if(mimetype.startsWith("video")) {
    		Bitmap bm = ThumbnailUtils.createVideoThumbnail(path, Images.Thumbnails.MINI_KIND);
    		if(bm!=null)
    			bm = Bitmap.createScaledBitmap(bm, w, h, false);
    		return bm;
    	}else if(mimetype.startsWith("image")) {
		   	BitmapFactory.Options opts = new BitmapFactory.Options();

		   	opts.inJustDecodeBounds = true;
		   	BitmapFactory.decodeFile(path, opts);
		   	opts.inJustDecodeBounds = false;
		   	opts.inSampleSize = Math.max(opts.outWidth/w, opts.outHeight/h);
		   	return BitmapFactory.decodeFile(path, opts);		
    	}
    	
    	return null;
    }

    public static String getPath(File file) {
    	String path = file.getAbsolutePath();
    	int i = path.lastIndexOf(fs);
    	if(i==-1) {
    		throw new RuntimeException("invalid file name");
    	}
    	return path.substring(0, i);
    }
    
    public static String getFilenameNoPath(File file) {
    	String path = file.getAbsolutePath();
    	int i = path.lastIndexOf(fs);
    	if(i==-1) {
    		throw new RuntimeException("invalid file name");
    	}
    	return path.substring(i+1, path.length());
    }
    
	public static void rename(File file, String newname) {	
		file.renameTo(new File(getPath(file)+fs+newname));
	}
	
	public static String combinePath(File root, String name) {
		return root.getAbsolutePath()+fs+name;
	}

	public static void moveFiles(ArrayList<File> fileList, File targetPath) {
		for(File f: fileList) {
			moveFile(f, targetPath);
		}
	}

	public static void moveFile(File file, File targetPath) {
		String path = getPath(file);
        
		if(!path.equals(targetPath)) {
            file.renameTo(new File(getPath(targetPath)+fs+getFilenameNoPath(file)));		
		}
	}

	public static void copyFiles(ArrayList<File> fileList, File targetPath) throws IOException {
		for(File f: fileList) {
			copyFile(f, targetPath);
		}
	}

	public static void copyFile(File sourceFile, File targetPath) throws IOException {
	    File targetFile = new File(targetPath.getPath()+fs+getFilenameNoPath(sourceFile));
	    if(targetFile.exists())
	    	throw new IOException("File Exist");
	    
		FileInputStream input = new FileInputStream(sourceFile);  
		BufferedInputStream inBuff = new BufferedInputStream(input);  
			  
		FileOutputStream output = new FileOutputStream(targetFile);  
		BufferedOutputStream outBuff=new BufferedOutputStream(output);  
			          
		byte[] b = new byte[1024 * 5];  
		int len;  
		while ((len =inBuff.read(b)) != -1) {  
			outBuff.write(b, 0, len);  
		}  
		outBuff.flush();  
			          
		inBuff.close();  
		outBuff.close();  
		output.close();  
		input.close();  
    }  
			    // 复制文件夹   
			    public static void copyDirectiory(String sourceDir, String targetDir)  
			            throws IOException {  
			        // 新建目标目录   
			        (new File(targetDir)).mkdirs();  
			        // 获取源文件夹当前下的文件或目录   
			        File[] file = (new File(sourceDir)).listFiles();  
			        for (int i = 0; i < file.length; i++) {  
			            if (file[i].isFile()) {  
			                // 源文件   
			                File sourceFile=file[i];  
			                // 目标文件   
			               File targetFile=new   
			File(new File(targetDir).getAbsolutePath()  
			+File.separator+file[i].getName());  
			                copyFile(sourceFile,targetFile);  
			            }  
			            if (file[i].isDirectory()) {  
			                // 准备复制的源文件夹   
			                String dir1=sourceDir + "/" + file[i].getName();  
			                // 准备复制的目标文件夹   
			                String dir2=targetDir + "/"+ file[i].getName();  
			                copyDirectiory(dir1, dir2);  
			            }  
			        }  
			    }  
}
