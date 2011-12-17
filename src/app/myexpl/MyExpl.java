package app.myexpl;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;

import android.view.*;
import android.widget.*;

public class MyExpl extends Activity
{
    private TextView statusBar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.statusBar=(TextView)findViewById(R.id.status_bar);
    }

    public void showStatus(int r) {
        this.statusBar.setText(r);
    }

    public void showStatus(String s) {
        this.statusBar.setText(s);
    }

    public void onHomeClick(View view) {
        this.showStatus(R.string.cmd_home);
    }

    public void onUpClick(View view) {
        this.showStatus(R.string.cmd_up);
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
