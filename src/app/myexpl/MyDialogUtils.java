package app.myexpl;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

class MyDialogUtils {
	private Activity app;
	String inputDialogInitText;
	EditText edit;
	
	MyDialogUtils(Activity app) {
		this.app = app;
	}
	
	/**
	 * create the input dialog with a EditText, please get it from {@link #edit} immediately
	 * 
	 * @param titleId
	 * @param listener
	 * @return the created dialog
	 */
    Dialog createInputDialog(int titleId, DialogInterface.OnClickListener listener) {
    	edit = new EditText(app);
    	edit.setId(1);
    	edit.setInputType(InputType.TYPE_CLASS_TEXT);
        return new AlertDialog.Builder(app)
        	.setTitle(titleId)
        	.setCancelable(true)
        	.setView(edit)
        	.setPositiveButton(R.string.info_ok, listener)
        	.setNegativeButton(R.string.info_cancel, null)
        	.create();
    }
    
    Dialog createSelectList(int titleId, int itemsId, DialogInterface.OnClickListener listener) {
    	return new AlertDialog.Builder(app)
    	    .setTitle(titleId)
    	    .setCancelable(true)
    	    .setSingleChoiceItems(itemsId, -1, listener)
    	    .create();
    }
    
    void showDialog(int id, String initText) {
    	inputDialogInitText = initText;
    	app.showDialog(id);
    }
    
    EditText getInputDialogText(Dialog dialog) {
    	return (EditText)dialog.findViewById(1);
    }    
    
    void putInputDialogInitState(Dialog dialog) {
    	getInputDialogText(dialog).setText(inputDialogInitText);
    }
}
