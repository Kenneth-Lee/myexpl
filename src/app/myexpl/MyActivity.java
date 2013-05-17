package app.myexpl;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class MyActivity extends Activity {
    protected Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if(!MyActivity.this.handleMessage(msg)) {
				super.handleMessage(msg);
			}
		}
    };
    
    protected boolean handleMessage(Message msg) {
    	return false;
    }
    
    public void qmsg(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    public void qmsg(int id) {    	
		Toast.makeText(this, id, Toast.LENGTH_LONG).show();
    }
    

}
