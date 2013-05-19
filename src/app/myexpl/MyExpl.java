package app.myexpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import app.myexpl.MyDialogUtils.MyInputDialogHelper;
import app.myexpl.MyDialogUtils.MyOKListener;

public class MyExpl extends MyActivity
{
    private TextView statusBar;
	private ListView fileListView;

	private static final int OMENU_MKDIR = 0;
	private static final int OMENU_CREATE_FILE = 1;
    private static final int OMENU_CLICK_FOR = 2;
    private static final int OMENU_PASTE_BUF = 3;
    private static final int OMENU_MOVE_BUF = 4;
    private static final int OMENU_MASK_BUF = 5;
    
    private ArrayList<File> buf1 = new ArrayList<File>();
    private ArrayList<File> buf2 = new ArrayList<File>();
    private ArrayList<File> curBuf = null;
    private ArrayList<File>[] bufList;
    
    private File currentRoot;
    private File[] currentFiles;
    private File selectedFile;
    
    private MyDialogUtils dialogUtils = new MyDialogUtils(this);
    
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
	private boolean isPreview = false;
	
    private void addHistory(String path, int x, int y) {
    	logger.push(new HistoryItem(path, x, y));
    	isPreExit=false;
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
        		for(File f: buf) {
        			if(!curBuf.contains(f)) {
        				curBuf.add(f);
        			}
        		}
        		refresh();
        	}
        }
        
        refresh();
    }
    
    private boolean isRefreshOnDemand = true;
    
    /** refresh file list only if isRefreshOnDemand is not set */
    public void lazyRefresh() {
    	if(!isRefreshOnDemand) {
    		refresh();
    	}
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
    
    private void setFileList(final File[] list) {
    	final Dialog pd = dialogUtils.createProgressDialog("Please wait");
    	pd.show();
    	asynDo(0, new MyMsgObject() {
			public void doMsg() {
				setFileListSync(list);
				pd.dismiss();
			}
    		
    	});
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
        
        /*
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
		*/
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
            	Bitmap bm = null;
            if(isPreview) {
            	bm = Utils.loadThumbnailImage(f, 72, 72);
            }
            
            if(bm==null) {
            	map.put("icon", Utils.getFileIcon(f.getName()));
            }else {
            	map.put("icon", bm);
            }
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

        buf1Btn.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				if(clickMode == ClickMode.CLICK4BUF1) {
					setClickMode(ClickMode.CLICK4OPEN);
				}else {
					setClickMode(ClickMode.CLICK4BUF1);
				}
				return false;
			}
        });
        
        buf2Btn.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				if(clickMode == ClickMode.CLICK4BUF2) {
					setClickMode(ClickMode.CLICK4OPEN);
				}else {
					setClickMode(ClickMode.CLICK4BUF2);
				}
				return false;
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
			return super.onKeyDown(keyCode, event);
	}

	public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.fmenu_mask:
        	MaskTo mt = MaskTo.getWin(this);
        	mt.show(selectedFile, this.fileListView);
        	mt.setFinishListener(new FinishListener() {
        		public void onFinish() {
        			asynDo(0, new MyMsgObject() {
						public void doMsg() {
							refresh();
						}
        			});
        		}
        	});
        	return true;
            
        case R.id.fmenu_headview:
        	HeadHexViewer hhv = HeadHexViewer.getViewer(this);
        	hhv.show(selectedFile, this.fileListView);
        	return true;
        	
        case R.id.fmenu_delete:
            	dialogUtils.createComfirmDialog(new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						try {
							Utils.deleteDir(selectedFile);
							if(curBuf!=null) {
								curBuf.remove(selectedFile);
							}
						}catch(Exception e) {
							showException(e);
						}
						refresh();
					}
            	}).show();
            	return true;
            	
            case R.id.fmenu_rename:
            	final MyInputDialogHelper dlg = dialogUtils.createInputDialog(R.string.info_rename);
            	dlg.edit.setText(selectedFile.getName());
            	dlg.setOKListener(new MyDialogUtils.MyOKListener() {
            		public void onOK(String text) {
            			if(text.length()==0) return;
                     try {
                    	 Utils.rename(selectedFile, text);
                     }catch(Exception e) {
                    	 showException(e);
                     }finally {
                    	 refresh();
                        }
                     	
					}
            	});
            	dlg.dialog.show();
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
        menu.add(0, OMENU_MASK_BUF, 0, getString(R.string.cmd_mask_buf));
    	return super.onCreateOptionsMenu(menu);
    }
    
    private int omenu_item = 0;
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case OMENU_MKDIR:            	
            {
                final MyInputDialogHelper dlg = dialogUtils.createInputDialog(R.string.cmd_mkdir);
                dlg.setOKListener(new MyOKListener() {
						public void onOK(String text) {
                        	if(text.length()==0) return;
                        	try {
                        		File file = new File(Utils.combinePath(currentRoot, text));
                        		file.mkdir();
                        	}catch(Exception e) {
                        		showException(e);
                        	}finally {
                        		refresh();
                        	}
						}
                });
                dlg.dialog.show();
            }
                return true;
                
            case OMENU_CREATE_FILE:
            {
                final MyInputDialogHelper dlg = dialogUtils.createInputDialog(R.string.cmd_createfile);
                dlg.setOKListener(new MyOKListener() {
						public void onOK(String text) {
                        	if(text.length()==0) return;
                        	try {
                        		File file = new File(Utils.combinePath(currentRoot, text));
                        		file.createNewFile();
                        	}catch(Exception e) {
                        		qmsg(getString(R.string.info_exception) + e.getMessage());
                        	}finally {
                        		refresh();
                            }
						}
                    });
                dlg.dialog.show();
            }
                return true;
            	
            case OMENU_CLICK_FOR:
            	dialogUtils.createSelectList(R.string.cmd_clickfor, R.array.cmd_click_for_what, 
            			new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
                        setClickMode(ClickMode.fromInt(item));
                        dialog.dismiss();
            	    }
            	}).show();
                return true;
                
            case OMENU_PASTE_BUF:
            case OMENU_MOVE_BUF:
            	int title_id = omenu_item==OMENU_PASTE_BUF?R.string.cmd_paste_buf:R.string.cmd_move_buf;
            	omenu_item = item.getItemId();
            	dialogUtils.createSelectList(title_id, R.array.cmd_buffers, new OnClickListener() {
            		public void onClick(DialogInterface dialog, int item) {
            			assert(item<2);
            			paste_move_buffer(bufList[item]);
            			dialog.dismiss();
            	    }
            	}).show();
            	return true;
                
            case OMENU_MASK_BUF:
            	if(this.curBuf == null) {
            		qmsg("no buffer seleted");
            		return true;
            	}
            	
            	MaskTo mt = MaskTo.getWin(this);
            	mt.show(curBuf, this.fileListView);
            	mt.setFinishListener(new FinishListener() {
            		public void onFinish() {
            			asynDo(0, new MyMsgObject() {
            				public void doMsg() {
            					curBuf.clear();
            					curBuf = null;
            					setRoot(currentRoot, false);
            					
            				}
            			});
            		}
            	});
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

        if(currentRoot!=null) {
        	return;
        }
        
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
