package app.myexpl;

import java.io.FileInputStream;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class HeadHexViewer extends Activity {
	
	private int headSize = 256;
	private TextView statusBar;
	private EditText viewer;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hex_view);

        statusBar = (TextView)findViewById(R.id.hex_viewer_status_bar);
        viewer = (EditText)findViewById(R.id.hex_viewer);
        
        Uri uri = getIntent().getData();
        String path = uri.getPath();
        try {
        	FileInputStream fis = new FileInputStream(path);
        	byte[] buffer = new byte[headSize];
        	int size = fis.read(buffer);
        	fillViewer(buffer, size);
        	viewer.scrollTo(0, 0);
        	fis.close();
        }catch(Exception e) {
        	viewer.setText(getString(R.string.info_exception)+e.getMessage());
        }
        statusBar.setText(path);
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
