package app.myexpl;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MaskTo extends MyActivity {
    private static final int maskLen = 4096;
	private TextView statusBar;
	private EditText passEdit;
	private CheckBox fullMaskCheck;
	private String path;
	private ProgressDialog pd;
	private final static int DIALOG_PD_ID=0;
	private final static int M_PROGRESS=0;
	private final static int M_EXCEPTION=1;
	
	@Override
	protected boolean handleMessage(Message msg) {
		switch(msg.what) {
		case M_PROGRESS:
			pd.setProgress(msg.arg1);
			return true;
		case M_EXCEPTION:
			pd.dismiss();
			statusBar.setText(((Exception)msg.obj).getLocalizedMessage());
			return true;
		}
        return false;
	}
    
	protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
    	case DIALOG_PD_ID:
    		pd.setProgress(0);
    		return;
    	}
		super.onPrepareDialog(id, dialog);
	}
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_PD_ID:
            	pd = new ProgressDialog(this);
            	pd.setMessage(getString(R.string.info_on_masking));
            	pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            	pd.setCancelable(false);
            	return pd;
            default:
            	return super.onCreateDialog(id);
        }
	}

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mask_to_view);

        statusBar = (TextView)findViewById(R.id.status_bar);
        passEdit = (EditText)findViewById(R.id.password_edit);
        fullMaskCheck = (CheckBox)findViewById(R.id.if_full_mask_checkbox);
        
        Uri uri = getIntent().getData();
        path = uri.getPath();
        statusBar.setText(path);
        passEdit.setText("");
        fullMaskCheck.setChecked(path.endsWith(".klf"));
        
    }
 
    private void doFullMask(final String pass, final String path) {    	
    	showDialog(DIALOG_PD_ID);
    	Thread workThread = new Thread() {

			@Override
			public void run() {
				try {
					File file = new File(path);
					FileCipher.fullMask(pass, file, new FileCipher.MaskProgressListener() {
						
						@Override
						public void progressUpdate(int progress) {
							Message msg = new Message();
							msg.what = M_PROGRESS;
							msg.arg1 = progress;
							handler.sendMessage(msg);
						}
					});
					file.renameTo(new File(Utils.switchNameExt(path, "klf")));
				}catch(Exception e) {
					Message msg = new Message();
					msg.what = M_EXCEPTION;
					msg.obj = e;
                    handler.sendMessage(msg);
				}
				MaskTo.this.finish();
			}
    		
    	};
    	workThread.start();
    }
    
    private void doMask(String pass, String path) {
        try {
            File file = new File(path);
            FileCipher.mask(passEdit.getText().toString(), file, maskLen);
            file.renameTo(new File(Utils.switchNameExt(path, "kle")));
        }catch(Exception e) {
        	statusBar.setText(e.getLocalizedMessage());
        }
        qmsg(R.string.info_done_mask);
        finish();
    }
    
    public void onMaskClick(View view) {
    	String pass = passEdit.getText().toString().trim();
    	if(pass=="") {
    		qmsg(R.string.info_pass_first);
    	}
    	if(fullMaskCheck.isChecked()) {
    		doFullMask(pass, path);
    	}else {
    		doMask(pass, path);
    	}
 }

}
