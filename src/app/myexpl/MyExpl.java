package app.myexpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MyExpl extends MyActivity
{
    private TextView statusBar;
	private ListView fileListView;

    private static final int DIALOG_PASSWORD_ID = 0;
    private static final int DIALOG_DELETE_ID = 1;
    private static final int DIALOG_RENAME_ID = 2;
    private static final int DIALOG_CREATE_FILE_ID = 3;
    private static final int DIALOG_MKDIR_ID = 4;
    private static final int DIALOG_CLICK_FOR_ID = 5;
    private static final int DIALOG_PASTE_MOVE_BUF_ID = 6;
    
	private static final int OMENU_MKDIR = 0;
	private static final int OMENU_CREATE_FILE = 1;
    private static final int OMENU_CLICK_FOR = 2;
    private static final int OMENU_PASTE_BUF = 3;
    private static final int OMENU_MOVE_BUF = 4;
    
    private final int M_DISMISS_PROGRESS_DIALOG = 1;
    private final int M_SHOW_LIST = 2;
    private final int M_UPDATE_THUMB = 3;

    private ArrayList<File> buf1 = new ArrayList<File>();
    private ArrayList<File> buf2 = new ArrayList<File>();
    private ArrayList<File> curBuf = null;
    private ArrayList<File>[] bufList;
    
    private File currentRoot;
    private File[] currentFiles;
    private File selectedFile;
    
    private MyDialogUtils dialogUtils = new MyDialogUtils(this);
    
    private EditText passEdit,renameEdit,dirEdit,newfileEdit;
    private Button buf1Btn, buf2Btn;
    
    enum ClickMode {
    	CLICK4OPEN,
    	CLICK4BUF1,
    	CLICK4BUF2,
    	CLICK4SMARTRENAME;
        static ClickMode fromInt(int ord) {
        	for(ClickMode i: ClickMode.values()) {
        		if(i.ordinal()==ord) {
        			return i;
        		}
        	}
            throw new RuntimeException("Error Order " + ord);
        }
    }
    private ClickMode clickMode=ClickMode.CLICK4OPEN;
    
    private class HistoryItem {
    	public HistoryItem(String path, int x, int y) {
			this.x = x;
			this.y = y;
			this.path = path;
		}
		int x,y;
    	String path;
    }
    private Stack<HistoryItem> logger = new Stack<HistoryItem>();
    private boolean isPreExit = false;
	private boolean isShowThumb = false;
    private void addHistory(String path, int x, int y) {
    	logger.push(new HistoryItem(path, x, y));
    	isPreExit=false;
    }
    
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
    
    protected boolean handleMessage(Message msg) {
		switch(msg.what) {
		case M_DISMISS_PROGRESS_DIALOG:
			ProgressDialog pd = (ProgressDialog)msg.obj;
			pd.dismiss();
			return true;
		case M_SHOW_LIST:
			SetFileListData data = (SetFileListData)msg.obj;
			setFileListSync(data.list);
			Message rmsg = new Message();
			rmsg.what = M_DISMISS_PROGRESS_DIALOG;
			rmsg.obj = data.obj;
			handler.sendMessage(rmsg);
			return true;
		case M_UPDATE_THUMB:
			ThumbData tData = (ThumbData)msg.obj;
			Bitmap bm = Utils.loadThumbnailImage(tData.file, 72, 72);
			if(bm!=null) {
				tData.view.setImageBitmap(bm);
			}
			return true;
		}
        return false;
	}

    
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
							try {
									selectedFile.delete();
									if(curBuf!=null) {
										curBuf.remove(selectedFile);
									}
							}catch(Exception e) {
								showException(e);
							}
							refresh();
						}
					})
					.setNegativeButton("Cancel", null).create();
            	break;
            	
            case DIALOG_RENAME_ID:
                dialog = dialogUtils.createInputDialog(R.string.info_rename, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	String newname=renameEdit.getText().toString();
                        	newname=newname.trim();
                        	if(newname=="") return;
                            try {
                                Utils.rename(selectedFile, newname);
                                refresh();
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
                                refresh();
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
                                refresh();
                            }catch(Exception e) {
                                qmsg(getString(R.string.info_exception) + e.getMessage());
                                return;
                            }
                        }
                    });
                newfileEdit = dialogUtils.edit;
                break;

            case DIALOG_CLICK_FOR_ID:
            	dialog = dialogUtils.createSelectList(R.string.cmd_clickfor, R.array.cmd_click_for_what, 
            			new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
                        setClickMode(ClickMode.fromInt(item));
                        dialog.dismiss();
            	    }
            	});
                break;
                
            case DIALOG_PASTE_MOVE_BUF_ID:
                int title_id = omenu_item==OMENU_PASTE_BUF?R.string.cmd_paste_buf:R.string.cmd_move_buf;
                dialog = dialogUtils.createSelectList(title_id, R.array.cmd_buffers, 
            			new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
                        assert(item<2);
                        paste_move_buffer(bufList[item]);
                        dialog.dismiss();
            	    }
            	});
            	break;
                
            default:
                dialog = null;
                break;
        }
        return dialog;
    }
    
    private void paste_move_buffer(ArrayList<File> buf) {
        if(curBuf==null) {
            /* current list is a real directory */
        	if(omenu_item==OMENU_MOVE_BUF) {
        		Utils.moveFiles(buf, currentRoot);
        	}else {
                assert(omenu_item==OMENU_PASTE_BUF);
                try {
                    Utils.copyFiles(buf, currentRoot);
                }catch(IOException e) {
                    showException(e);
                }
        	}
        }else {
        	if(curBuf!=buf) {
        		
        	}
        }
        
        refresh();
    }
    
	public void refresh() {
		if(curBuf==null) {
			setRoot(currentRoot, false);			
		}else {
			setFileList(curBuf);
		}
	}
	
    public void setRoot(File root, boolean isLog) {
        if(root==null || !root.exists())
            return;

        curBuf=null;
        if(isLog && currentRoot!=null && currentRoot.getAbsolutePath()!=root.getAbsolutePath()) {
        	addHistory(currentRoot.getAbsolutePath(), this.fileListView.getScrollX(), this.fileListView.getScrollY());
        }
        currentRoot = root;
        currentFiles = root.listFiles();

        sortFileListByName(currentFiles);
        setFileList(currentFiles);
    }

    private void sortFileListByName(File[] list) {
        if(list!=null) {
            Arrays.sort(list, Utils.genFileNameComparator(true)); 
        }
    }

    private void sortFileListByTime(File[] list) {
        if(list!=null) {
            Arrays.sort(list, Utils.genFileTimeComparator(true)); 
        }
    }

    private void setFileList(ArrayList<File> list) {
    	setFileList(genFileList(list));
    }
    
    private void setFileList(File[] list) {
    	handler.removeMessages(M_UPDATE_THUMB);
    	ProgressDialog pd = new ProgressDialog(this);
    	pd.setMessage("Please wait");
    	pd.show();
        if(isShowThumb ) {
    	    Message msg = new Message();
    	    SetFileListData data = new SetFileListData(list, pd);
        	msg.what = M_SHOW_LIST;
        	msg.obj = data;
        	handler.sendMessage(msg);
        }
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
    
    private String genFileInfo(File f) {
    	String info = Utils.fileSize(f.length());
        if(buf1.contains(f)) {
        	info=info+"(b1)";
        }
        if(buf2.contains(f)) {
        	info=info+"(b2)";
        }
        return info;
    }
    
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
            map.put("info", genFileInfo(f));
            map.put("icon", new ThumbData(f));
        }
        map.put("filename", f.getName());
        return map;
    }
    
    private String getListType() {
    	if(curBuf==null) {
    		return "("+currentRoot.getAbsolutePath()+")";    		
    	}else if(curBuf==buf1) {
    		return "(buffer1)";
    	}else if(curBuf==buf2) {
    		return "(buffer2)";
    	}
    	return "(error)";
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        bufList = new ArrayList[2];
        bufList[0] = buf1;
        bufList[1] = buf2;
        
        setContentView(R.layout.main);

        this.statusBar=(TextView)findViewById(R.id.status_bar);
        fileListView = (ListView)findViewById(R.id.file_list);
        buf1Btn = (Button)findViewById(R.id.buf1);
        buf2Btn = (Button)findViewById(R.id.buf2);
        
        registerForContextMenu(fileListView);
        
        fileListView.setItemsCanFocus(false);
        
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
            	if(curBuf!=null) {
            		return; //No Dir in buffer list
            	}
            	
                if(currentFiles[i].isDirectory()) {
                    setRoot(currentFiles[i], true);
                }else {
                    switch(clickMode) {
                    case CLICK4OPEN:
                    	try {
                    		Utils.openFileWithApp(MyExpl.this, currentFiles[i]);
                    	}catch(Exception e) {
                    		qmsg(getString(R.string.info_exception)+e.getMessage());
                    	}
                        break;
                    case CLICK4BUF1:
                    	if(buf1.contains(currentFiles[i])) {
                    		buf1.remove(currentFiles[i]);
                    	}else {
                    		buf1.add(currentFiles[i]);
                    	}
                        ((TextView)v.findViewById(R.id.fileinfo)).setText(genFileInfo(currentFiles[i]));
                        break;
                    case CLICK4BUF2:
                    	if(buf2.contains(currentFiles[i])) {
                    		buf2.remove(currentFiles[i]);
                    	}else {
                    		buf2.add(currentFiles[i]);
                    	}
                        ((TextView)v.findViewById(R.id.fileinfo)).setText(genFileInfo(currentFiles[i]));
                        break;
                    default:
                    	break;
                    }
                }
            }
        });

        setRoot(Environment.getExternalStorageDirectory(), true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, 
            ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId()==R.id.file_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            selectedFile = currentFiles[info.position];
            menu.setHeaderTitle(selectedFile.getName());
            MenuInflater inflater = getMenuInflater();
            if(selectedFile.isFile()) {
                inflater.inflate(R.menu.file_option, menu);
            }else {
            	inflater.inflate(R.menu.dir_option, menu);
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
        setRoot(Environment.getExternalStorageDirectory(), true);
    }
    
    public void onHomeClick(View view) {
        goHome();
    }

    public void onUpClick(View view) {
    	setRoot(currentRoot.getParentFile(), true);
    }
    
    public void onRefreshClick(View view) {
    	refresh();
    }
    
    private File[] genFileList(ArrayList<File> a) {
    	File[] files = new File[a.size()];
    	for(int i=0; i<a.size(); i++) {
    		files[i] = a.get(i);
    	}
    	return files;
    }
    
    public void onBuf1Click(View view) {
        if(curBuf==buf1) {
        	curBuf=null;
        	setFileList(currentFiles);
        }else {
        	curBuf=buf1;
        	setFileList(curBuf);
        }
    }

    public void onBuf2Click(View view) {
        if(curBuf==buf2) {
        	curBuf=null;
        	setFileList(currentFiles);
        }else {
        	curBuf=buf2;
        	setFileList(curBuf);
        }
    }
    
    private void setClickMode(ClickMode mode) {
        if(clickMode==mode) {
        	return;
        }
        
    	switch(mode) {
    	case CLICK4OPEN:
            buf1Btn.setTextColor(Color.BLACK);
            buf2Btn.setTextColor(Color.BLACK);
            break;
    	case CLICK4BUF1:
            buf1Btn.setTextColor(Color.RED);
            buf2Btn.setTextColor(Color.BLACK);
            break;
    	case CLICK4BUF2:
            buf1Btn.setTextColor(Color.BLACK);
            buf2Btn.setTextColor(Color.RED);
            break;
    	case CLICK4SMARTRENAME:
            break;
    	default:
    		assert(false);
    	    break;		
    	}
        clickMode=mode;
    }
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK) {
            if(clickMode!=ClickMode.CLICK4OPEN) {
                setClickMode(ClickMode.CLICK4OPEN);
                return true;
            }else if(logger.isEmpty()) {
				if(isPreExit) {
					finish();
					return true;
				}else {
					qmsg(getString(R.string.info_to_be_exit));
					isPreExit = true;
					return true;
				}
			}else {
				HistoryItem log = logger.pop();
				setRoot(new File(log.path), false);
				fileListView.scrollTo(log.x, log.y);
				return true;
			}
		}else 
			return super.onKeyUp(keyCode, event);
	}

	public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.fmenu_delete:
                showDialog(DIALOG_DELETE_ID);
                return true;
            case R.id.fmenu_rename:
            	dialogUtils.showDialog(DIALOG_RENAME_ID, selectedFile.getName());
            	return true;
            case R.id.fmenu_headview:
                //TODO
            	return true;
            case R.id.fmenu_mask:
                //TODO
            	return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, OMENU_MKDIR, 0, getString(R.string.cmd_mkdir));
    	menu.add(0, OMENU_CREATE_FILE, 0, getString(R.string.cmd_createfile));
        menu.add(0, OMENU_CLICK_FOR, 0, getString(R.string.cmd_clickfor));
        menu.add(0, OMENU_PASTE_BUF, 0, getString(R.string.cmd_paste_buf));
        menu.add(0, OMENU_MOVE_BUF, 0, getString(R.string.cmd_move_buf));
    	return super.onCreateOptionsMenu(menu);
    }
    
    private int omenu_item = 0;
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case OMENU_MKDIR:            	
            	dialogUtils.showDialog(DIALOG_MKDIR_ID, "");
                return true;
            case OMENU_CREATE_FILE:
            	dialogUtils.showDialog(DIALOG_CREATE_FILE_ID, "");
            	return true;
            case OMENU_CLICK_FOR:
                showDialog(DIALOG_CLICK_FOR_ID);
                return true;
            case OMENU_PASTE_BUF:
            case OMENU_MOVE_BUF:
                omenu_item = item.getItemId();
                showDialog(DIALOG_PASTE_MOVE_BUF_ID);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    setRoot(nr, false);
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
