package app.myexpl;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;

import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;

public class MyExpl extends Activity
{
    private TextView statusBar;
    private ListView fileListView;
    private int fileListChoiceMode = ListView.CHOICE_MODE_NONE;
    private File currentRoot;
    private File[] currentFiles;
    private final String rootDir="/sdcard";

    public void setNewRoot(File root) {
        if(root==null || !root.exists())
            return;

        currentRoot = root;
        currentFiles = root.listFiles();

        int len;
        if(currentFiles==null) {
            showStatus(R.string.info_nofile);
            len = 0;
        }else {
            len = currentFiles.length;
        }

        ArrayList<HashMap<String, Object>> fileList = 
            new ArrayList<HashMap<String, Object>>();

        for(int i=0; i<len; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            if(currentFiles[i].isDirectory()) {
                map.put("icon", R.drawable.folder);
            }else {
                map.put("icon", R.drawable.file);
            }
            map.put("filename", currentFiles[i].getName());
            fileList.add(map);
        }
        fileListView.setAdapter(new SimpleAdapter(this,
                    fileList, R.layout.file_list,
                    new String[] {"icon", "filename"},
                    new int[]{R.id.fileicon, R.id.filename}));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.statusBar=(TextView)findViewById(R.id.status_bar);
        this.fileListView=(ListView)findViewById(R.id.file_list);

        setNewRoot(new File(rootDir));
        fileListView.setItemsCanFocus(false);

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                if(currentFiles[i].isDirectory()) {
                    setNewRoot(currentFiles[i]);
                }
            }
        });
        registerForContextMenu(fileListView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, 
            ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId()==R.id.file_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.setHeaderTitle(currentFiles[info.position].getName());
            menu.add(Menu.NONE, 0, 0, getResources().getString(R.string.cmd_left));
            menu.add(Menu.NONE, 1, 1, getResources().getString(R.string.cmd_right));
        }
    }

    public void showStatus(int r) {
        this.statusBar.setText(r);
    }

    public void showStatus(String s) {
        this.statusBar.setText(s);
    }

    public void onHomeClick(View view) {
        setNewRoot(new File(rootDir));
    }

    public void onUpClick(View view) {
        File f = currentRoot.getParentFile();
        if(f!=null && f.exists()) {
            currentRoot = f;
            setNewRoot(f);
        }
    }

    public void onSelectModeClick(View view) {
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
    }

    /**
     * When pause, save the paremeter to SharedPreferences
     */
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString("status", statusBar.getText().toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getPreferences(0);
        String status = prefs.getString("status", null);
        if(status != null) {
            showStatus(status);
        }
    }
}

/* vim: set et ts=4 sw=4: */
