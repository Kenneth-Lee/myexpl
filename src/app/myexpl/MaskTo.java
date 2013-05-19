package app.myexpl;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MaskTo extends MyPopupWindowHelper {
    private static final int maskLen = 4096;
	private TextView statusBar;
	private EditText passEdit;
	private CheckBox fullMaskCheck;
	private ProgressDialog pd;
	private MyActivity a;
	private ArrayList<File> files;
	boolean isUpdated;
	
	static MaskTo me = null;
	
	
    MaskTo(Activity app) {
    	super(app);
    	a = (MyActivity)app;
    	
       setRootView(R.layout.mask_to_view);

       statusBar = (TextView)rootView.findViewById(R.id.status_bar);
       passEdit = (EditText)rootView.findViewById(R.id.password_edit);
       fullMaskCheck = (CheckBox)rootView.findViewById(R.id.if_full_mask_checkbox);
        Button btn = (Button)rootView.findViewById(R.id.maskButton);
        btn.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View view) {
		    	String pass = passEdit.getText().toString().trim();
		    	if(pass.length()==0) {
		    		a.qmsg(R.string.info_pass_first);
		    	}else if(fullMaskCheck.isChecked()) {
		    		for(File file : files) {
		    			doFullMask(pass, file);
		    		}
		    	}else {
		    		for(File file : files) {
		    			doMask(pass, file);
		    		}
		    	}
		    }
        });
    }
    
    public void show(ArrayList<File> f, View view) {
    	assert(f!=null);
    	int fn = f.size();
    	if(fn==0) {
    		a.qmsg("no file specific");
    		return;
    	}else if(fn==1) {
    		statusBar.setText(f.get(0).getName());
    	}else {
    		statusBar.setText(f.get(0).getName()+"...");
    	}
    	files = f;
    	passEdit.setText("");
    	fullMaskCheck.setChecked(f.get(0).getName().endsWith(".klf"));
    	isUpdated = false;
    	MyPopupWindowHelper.show(this, view);
    }
    
    public void show(File f, View view) {
    	 files = new ArrayList<File>();
    	 files.add(f);
    	 show(files, view);
     }
    
    private void doFullMask(final String pass, final File file) {    	
    	pd = new ProgressDialog(app);
    	pd.setMessage(app.getString(R.string.info_on_masking));
    	pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    	pd.setCancelable(false);
    	pd.setProgress(0);
    	
    	Thread workThread = new Thread() {

			@Override
			public void run() {
				try {
					FileCipher.fullMask(pass, file, new FileCipher.MaskProgressListener() {
						public void progressUpdate(int progress) {
							final int p = progress;
							a.asynDo(0, new MyMsgObject() {
								public void doMsg() {
									pd.setProgress(p);
								}
							});
						}
					});
					file.renameTo(new File(Utils.switchNameExt(file.getAbsolutePath(), "klf")));
					isUpdated = true;
				}catch(Exception e) {
					final Exception ee = e;
					a.asynDo(0, new MyMsgObject() {
						public void doMsg() {
							MyPopupWindowHelper.dismiss(true);
							a.qmsg(ee.getLocalizedMessage());
						}
					});
				}
				a.asynDo(0, new MyMsgObject() {
					public void doMsg() {
						MyPopupWindowHelper.dismiss(true);
					}
				});
			}
    		
    	};
    	workThread.start();
    }
    
    private void doMask(String pass, File file) {
    	try {
    		FileCipher.mask(passEdit.getText().toString(), file, maskLen);
    		file.renameTo(new File(Utils.switchNameExt(file.getAbsolutePath(), "kle")));
    		isUpdated = true;
    		a.qmsg(R.string.info_done_mask);
    	}catch(Exception e) {
        	a.qmsg(e.getLocalizedMessage());
        }finally {
        	MyPopupWindowHelper.dismiss(true);
        }
    }
    
	public static MaskTo getWin(Activity app) {
		if(me==null) {
			me = new MaskTo(app);
		}
		
		return me;
	}

}
