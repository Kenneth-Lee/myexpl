package app.myexpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.widget.EditText;

class MyDialogUtils {
	private Activity app;
	String inputDialogInitText;
	final int EDIT_ID = 1;
	
	MyDialogUtils(Activity app) {
		this.app = app;
	}
	
	interface MyOKListener {
		void onOK(String text);
	}
	
	class MyInputDialogHelper implements OnClickListener {
		Dialog dialog;
		EditText edit;
		MyOKListener listener;
		
		MyInputDialogHelper(Activity app, int titleId) {
			edit = new EditText(app);
	    	edit.setId(EDIT_ID);
	    	edit.setInputType(InputType.TYPE_CLASS_TEXT);
	       dialog = new AlertDialog.Builder(app)
	        	.setTitle(titleId)
	        	.setCancelable(true)
	        	.setView(edit)
	        	.setPositiveButton(R.string.info_ok, this)
	        	.setNegativeButton(R.string.info_cancel, null)
	        	.create();
		}

		public void onClick(DialogInterface arg0, int arg1) {
			 listener.onOK(edit.getText().toString().trim());
		}
		
		public void setOKListener(MyOKListener listener) {
			this.listener = listener;
		}
	}
	
	/**
	 * create the input dialog with a EditText, please get it from {@link #edit} immediately
	 * 
	 * @param titleId
	 * @param listener
	 * @return the created dialog
	 */
    MyInputDialogHelper createInputDialog(int titleId) {
    	return new MyInputDialogHelper(app, titleId);
    }
    
    Dialog createSelectList(int titleId, int itemsId, DialogInterface.OnClickListener listener) {
    	return new AlertDialog.Builder(app)
    	    .setTitle(titleId)
    	    .setCancelable(true)
    	    .setSingleChoiceItems(itemsId, -1, listener)
    	    .create();
    }
    
    ProgressDialog createProgressDialog(String msgId) {
    	ProgressDialog pd = new ProgressDialog(app);
    	pd.setMessage(msgId);
		return pd;
    }
    
    AlertDialog createComfirmDialog(OnClickListener listener) {
    	 return new AlertDialog.Builder(app)
	    .setMessage("Are you sure?")
	    .setCancelable(true)
	    .setPositiveButton("OK", listener)
	    .setNegativeButton("Cancel", null)
	    .create();
    }
    
    void showDialog(int id, String initText) {
    	inputDialogInitText = initText;
    	app.showDialog(id);
    }
    
}
