package app.myexpl;

import java.io.File;
import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
    	if(msg.obj instanceof MyMsgObject) {
    		MyMsgObject mmo = (MyMsgObject)msg.obj;
    		mmo.doMsg();
    		return true;
    	}
    	return false;
    }
    
    void asynDo(int what, MyMsgObject msgObj) {
	    Message msg = new Message();
    	msg.what = what;
    	msg.obj = msgObj;
    	handler.sendMessage(msg);
    }
    
    public void startSpecificActivity(Class activity, File f) {
//        Intent intent = new Intent(this, activity);
        Intent intent = new Intent(android.content.Intent.ACTION_CALL);
//        intent.setDataAndType(Uri.fromFile(f), Utils.getMimeType(f.getName()));
        intent.setDataAndType(Uri.fromFile(f), "binary/binary");
        intent.setClass(this, activity);
        startActivity(intent);
    }
    
    public void qmsg(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    public void qmsg(int id) {    	
		Toast.makeText(this, id, Toast.LENGTH_LONG).show();
    }
    
    public void showException(Exception e) {
    	qmsg(getString(R.string.info_exception)+e.getMessage());
    }

}
