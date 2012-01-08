package app.myexpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MyExpl extends Activity
{
    private TextView statusBar;
	private ListView fileListView;

    private static final int maskLen = 4096;
    private static final int DIALOG_PASSWORD_ID = 0;
    private static final int DIALOG_DELETE_ID = 1;
    private static final int DIALOG_RENAME_ID = 2;
    private static final int DIALOG_CREATE_FILE_ID = 3;
    private static final int DIALOG_MKDIR_ID = 4;
    
	private static final int OMENU_MKDIR = 0;
	private static final int OMENU_CREATE_FILE = 1;
    
    private final int LID_FS=0;
    private final int LID_BUF1=1;
    private final int LID_BUF2=2;
    private int whichList=LID_FS;
    
    private final int M_DISMISS_PROGRESS_DIALOG = 1;
    private final int M_SHOW_LIST = 2;
    private final int M_UPDATE_THUMB = 3;

    private Vector<File> buf1 = new Vector<File>();
    private Vector<File> buf2 = new Vector<File>();
    
    private File currentRoot;
    private File[] currentFiles;
    private File selectedFile;
    
    private MyDialogUtils dialogUtils = new MyDialogUtils(this);
    
    private EditText passEdit,renameEdit,dirEdit,newfileEdit;
    
    private class SetFileListData {
    	File[] list;
    	Object obj;
    	SetFileListData(File[] list, Object obj) {
    		this.list = list;
    		this.obj = obj;
    	}
    };
    
    private class ThumbData {
    	File file;
    	ImageView view;
    	ThumbData(File file) {
    		this.file = file;
    	}
    	void sendUpdateMessage(Handler h, ImageView v) {
    		view = v;
    		Message msg = new Message();
    		msg.what = M_UPDATE_THUMB;
    		msg.obj = this;
    		h.sendMessage(msg);
    	}
    }
    
    private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case M_DISMISS_PROGRESS_DIALOG:
				ProgressDialog pd = (ProgressDialog)msg.obj;
				pd.dismiss();
				return;
			case M_SHOW_LIST:
				SetFileListData data = (SetFileListData)msg.obj;
				setFileListSync(data.list);
				Message rmsg = new Message();
				rmsg.what = M_DISMISS_PROGRESS_DIALOG;
				rmsg.obj = data.obj;
				handler.sendMessage(rmsg);
				return;
			case M_UPDATE_THUMB:
				ThumbData tData = (ThumbData)msg.obj;
				Bitmap bm = Utils.loadThumbnailImage(tData.file, 72, 72);
				if(bm!=null) {
					tData.view.setImageBitmap(bm);
				}
				return;
			}
			super.handleMessage(msg);
		}
    };

    
    @Override
	protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
    	case DIALOG_PASSWORD_ID:
    	case DIALOG_RENAME_ID:
    	case DIALOG_CREATE_FILE_ID:
    	case DIALOG_MKDIR_ID:
    		dialogUtils.putInputDialogInitState(dialog);
    		return;
    	}
		super.onPrepareDialog(id, dialog);
	}

    private void showException(Exception e) {
    	qmsg(getString(R.string.info_exception)+e.getMessage());
    }
    
	protected Dialog onCreateDialog(int id) {
        Dialog dialog;

        switch(id) {
            case DIALOG_PASSWORD_ID:
                dialog = dialogUtils.createInputDialog(R.string.info_setmask,new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                FileCipher.mask(passEdit.getText().toString(), selectedFile, maskLen);
                                selectedFile.renameTo(new File(Utils.switchNameExt(selectedFile.getAbsolutePath(), "kle")));
                                setRoot(currentRoot);
                            }catch(Exception e) {
                                showException(e);
                                return;
                            }
                            qmsg("done mask");
                            passEdit.setText("");
                        }
                });
                passEdit = dialogUtils.edit;
                break;

            case DIALOG_DELETE_ID:
            	dialog = new AlertDialog.Builder(this)
            	    .setMessage("are you sure?")
            	    .setCancelable(true)
            	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectedFile.delete();
							setRoot(currentRoot);
						}
					})
					.setNegativeButton("Cancel", null).create();
            	
            case DIALOG_RENAME_ID:
                dialog = dialogUtils.createInputDialog(R.string.info_rename, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	String newname=renameEdit.getText().toString();
                        	newname=newname.trim();
                        	if(newname=="") return;
                            try {
                                Utils.rename(selectedFile, newname);
                                setRoot(currentRoot);
                            }catch(Exception e) {
                                showException(e);
                                return;
                            }
                        }
                    });
                renameEdit = dialogUtils.edit;
                break;
            
            case DIALOG_MKDIR_ID:
                dialog = dialogUtils.createInputDialog(R.string.cmd_mkdir, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	String name=dirEdit.getText().toString();
                        	name=name.trim();
                        	if(name=="") return;
                            try {
                            	File file = new File(Utils.combinePath(currentRoot, name));
                            	file.mkdir();
                                setRoot(currentRoot);
                            }catch(Exception e) {
                                showException(e);
                                return;
                            }
                        }
                    });
                dirEdit = dialogUtils.edit;
                break;
                
            case DIALOG_CREATE_FILE_ID:
                dialog = dialogUtils.createInputDialog(R.string.cmd_createfile, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	String name=newfileEdit.getText().toString();
                        	name=name.trim();
                        	if(name=="") return;
                            try {
                            	File file = new File(Utils.combinePath(currentRoot, name));
                                file.createNewFile();
                                setRoot(currentRoot);
                            }catch(Exception e) {
                                qmsg(getString(R.string.info_exception) + e.getMessage());
                                return;
                            }
                        }
                    });
                newfileEdit = dialogUtils.edit;
                break;

            default:
                dialog = null;
                break;
        }
        return dialog;
    }
    
    public void setRoot(File root) {
        if(root==null || !root.exists())
            return;

        currentRoot = root;
        currentFiles = root.listFiles();

        sortFileListByName(currentFiles);
        setFileList(currentFiles);
    }

    private void sortFileListByName(File[] list) {
        Arrays.sort(list, Utils.genFileNameComparator(true)); 
    }

    private void sortFileListByTime(File[] list) {
        Arrays.sort(list, Utils.genFileTimeComparator(true)); 
    }

    private void setFileList(File[] list) {
    	handler.removeMessages(M_UPDATE_THUMB);
    	ProgressDialog pd = new ProgressDialog(this);
    	pd.setMessage("Please wait");
    	pd.show();
    	Message msg = new Message();
    	SetFileListData data = new SetFileListData(list, pd);
    	msg.what = M_SHOW_LIST;
    	msg.obj = data;
    	handler.sendMessage(msg);
    }
    
    private void setFileListSync(File[] list) {
        int len;
        if(list==null) {
            len = 0;
        }else {
            len = list.length;
        }
        
        ArrayList<HashMap<String, Object>> fileList = 
                new ArrayList<HashMap<String, Object>>();

        for(int i=0; i<len; i++) {
            fileList.add(genFileItemMap(list[i]));
        }
        
        SimpleAdapter adapter = new SimpleAdapter(this,
                fileList, R.layout.file_list,
                new String[] {"icon", "filename", "info"},
                new int[]{R.id.fileicon, R.id.filename, R.id.fileinfo});
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Object data,
					String textRepresentation) {
				if(data instanceof ThumbData) {
					((ImageView)view).setImageResource(R.drawable.file);
					ThumbData d = (ThumbData)data;
					d.sendUpdateMessage(handler, (ImageView)view);
					return true;
				}
				return false;
			}
		});
        fileListView.setAdapter(adapter);    	
        showStatus(len+" items"+getListType());
    }
    
    /*
    private void setFileThumbnail(HashMap<String, Object> map, File f) {
    	Bitmap bm = Utils.loadThumbnailImage(f, 72, 72);
    	if(bm!=null) {
    		map.put("icon", bm);
    	}else {
            map.put("icon", R.drawable.file);    		
    	}
    }
    */
    
    private HashMap<String, Object> genFileItemMap(File f) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        if(f.isDirectory()) {
            map.put("icon", R.drawable.folder);
            File[] sublist=f.listFiles();
            if(sublist==null) {
            	map.put("info", "0 item");
            }else {
            	map.put("info", sublist.length+" items");                    	
            }
        }else {
            map.put("info", Utils.fileSize(f.length()));
            map.put("icon", new ThumbData(f));
            //setFileThumbnail(map, f);
        }
        map.put("filename", f.getName());
        return map;
    }
    
    private String getListType() {
    	switch(whichList) {
    	case LID_FS:
    		return "("+currentRoot.getAbsolutePath()+")";
    	case LID_BUF1:
    		return "(buffer1)";
    	case LID_BUF2:
    		return "(buffer2)";
    	default:
    		return "(error)";
    	}
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.statusBar=(TextView)findViewById(R.id.status_bar);
        fileListView = (ListView)findViewById(R.id.file_list);
        registerForContextMenu(fileListView);
        
        fileListView.setItemsCanFocus(false);
        
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
            	if(whichList!=LID_FS) {
            		return; //No Dir in buffer list
            	}
            	
                if(currentFiles[i].isDirectory()) {
                    setRoot(currentFiles[i]);
                }else {
                	try {
                		Utils.openFileWithApp(MyExpl.this, currentFiles[i]);
                	}catch(Exception e) {
                		qmsg(getString(R.string.info_exception)+e.getMessage());
                	}
                }
            }
        });

        setRoot(Environment.getExternalStorageDirectory());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, 
            ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId()==R.id.file_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            selectedFile = currentFiles[info.position];
            if(selectedFile.isFile()) {
                menu.setHeaderTitle(selectedFile.getName());
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.file_option, menu);
            }
        }
    }

    public void showStatus(int r) {
        this.statusBar.setText(r);
    }

    public void showStatus(String s) {
        this.statusBar.setText(s);
    }

    private void goHome() {
        setRoot(Environment.getExternalStorageDirectory());
    }
    
    public void onHomeClick(View view) {
        goHome();
    }

    public void onUpClick(View view) {
    	setRoot(currentRoot.getParentFile());
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.fmenu_left:
                dialogUtils.showDialog(DIALOG_PASSWORD_ID, "");
                return true;
            case R.id.fmenu_delete:
                showDialog(DIALOG_DELETE_ID);
                return true;
            case R.id.fmenu_rename:
            	dialogUtils.showDialog(DIALOG_RENAME_ID, selectedFile.getName());
            	return true;
            case R.id.fmenu_open:
            	try {
            		Utils.openFileWithApp(this, selectedFile);
            	}catch(Exception e) {
            		qmsg(getString(R.string.info_exception)+e.getMessage());
            	}
            	return true;
            	
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void qmsg(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, OMENU_MKDIR, 0, getString(R.string.cmd_mkdir));
    	menu.add(0, OMENU_CREATE_FILE, 0, getString(R.string.cmd_createfile));
    	return super.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case OMENU_MKDIR:            	
            	dialogUtils.showDialog(DIALOG_MKDIR_ID, "");
                return true;
            case OMENU_CREATE_FILE:
            	dialogUtils.showDialog(DIALOG_CREATE_FILE_ID, "");
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	public void onSelectModeClick(View view) {
    	/*
        switch(fileListChoiceMode) {
            case ListView.CHOICE_MODE_NONE:
                fileListChoiceMode=ListView.CHOICE_MODE_SINGLE;
                fileListView.setItemsCanFocus(true);
                showStatus(R.string.info_single_choice_mode);
                break;
            case ListView.CHOICE_MODE_SINGLE:
                fileListChoiceMode=ListView.CHOICE_MODE_MULTIPLE;
                fileListView.setItemsCanFocus(true);
                showStatus(R.string.info_multi_choice_mode);
                break;
            default:
                fileListChoiceMode=ListView.CHOICE_MODE_NONE;
                fileListView.setItemsCanFocus(false);
                showStatus(R.string.info_none_choice_mode);
                break;
        }
        fileListView.setChoiceMode(fileListChoiceMode);
        */
    }

    /**
     * When pause, save the paremeter to SharedPreferences
     */
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString("root", currentRoot.getAbsolutePath());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getPreferences(0);
        String r = prefs.getString("root", null);
        if(r!=null) {
            try {
                File nr = new File(r);
                if(nr.exists() && nr.isDirectory()) {
                    setRoot(nr);
                }else {
                    goHome();
                }
             }catch(Exception e) {
                 qmsg("error: "+e.getMessage());
             }
        }
    }
}

/* vim: set et ts=4 sw=4: */
