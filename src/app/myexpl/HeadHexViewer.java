package app.myexpl;

import java.io.File;
import java.io.FileInputStream;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

public class HeadHexViewer extends MyPopupWindowHelper {
	
	private int headSize = 1024;
	private TextView statusBar;
	private EditText viewer;
	static private HeadHexViewer hvviewer = null;
	
    private HeadHexViewer(Activity app) {
    	super(app);
    	setRootView(R.layout.hex_view);
    	viewer = (EditText)rootView.findViewById(R.id.hex_viewer);
    	statusBar = (TextView)rootView.findViewById(R.id.hex_viewer_status_bar);
    }
    
    static HeadHexViewer getViewer(Activity app) {
    	if(hvviewer == null) {
    		hvviewer = new HeadHexViewer(app);
    	}
    	
    	return hvviewer;
    }
    
    /**
     * show the file head in the window
     * @param f the file to view
     * @param view the root view of parent window (cannot be null)
     */
    public void show(File f, View view) {
       try {
    	   FileInputStream fis = new FileInputStream(f);
    	   byte[] buffer = new byte[headSize];
    	   int size = fis.read(buffer);
    	   fillViewer(buffer, size);
    	   viewer.scrollTo(0, 0);
    	   fis.close();
        }catch(Exception e) {
        	viewer.setText(app.getString(R.string.info_exception)+e.getMessage());
        }
       statusBar.setText(f.getAbsolutePath()+"(size:"+f.length()+")");
       MyPopupWindowHelper.show(this, view);
    }
    
    private int cPerLine=8;
    private void fillViewer(byte[] buffer, int size) {
		StringBuilder hexPart = new StringBuilder(3*cPerLine);
		StringBuilder chrPart = new StringBuilder(1*cPerLine);
		int i;
    	for(i=0; i<size; i++) {
    		hexPart.append(hexIt(buffer[i]));
    		chrPart.append(chrIt(buffer[i]));
    		if((i%cPerLine)==(cPerLine-1)) {
    			viewer.append(hexPart.toString());
    			viewer.append("- ");
    			viewer.append(chrPart.toString());
    			viewer.append(System.getProperty("line.separator"));
    			hexPart.setLength(0);
    			chrPart.setLength(0);
    		}
    	}
    	if(i%16>0) {
			viewer.append(hexPart.toString());
			for(int j=0; j<cPerLine-i%cPerLine; j++)
			    viewer.append("   ");
			viewer.append(chrPart.toString());
    	}
    }

	private char chrIt(byte b) {
		if(b>=32 && b<127) {
			return (char)b;
		}
		
		return '.';
	}

	private String hexIt(byte b) {
		String o = Integer.toHexString(0xff & b);
		if(o.length()==1) {
			return "0"+o+" ";
		}
		return o + " ";
	}
}
