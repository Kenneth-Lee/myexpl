package app.myexpl;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;

class MyPopupWindowHelper {
	private static MyPopupWindowHelper winOnScreen = null;
	
	protected Activity app;
	protected PopupWindow window;
	protected View rootView;
	FinishListener finishListener;
	
	MyPopupWindowHelper(Activity app) {
		this.app = app;
	}
	
	protected void setRootView(int resId) {
		LayoutInflater li = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
		rootView = li.inflate(resId, null);
       window = new PopupWindow(rootView, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);  
       window.setBackgroundDrawable(new BitmapDrawable());
       rootView.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(keyCode==KeyEvent.KEYCODE_BACK) {
						MyPopupWindowHelper.dismiss(false);
						return true;
				}
				
				return false;
			}
       });
	}
	
	protected static void show(MyPopupWindowHelper w, View view) {
		winOnScreen = w;
		winOnScreen.window.showAtLocation(view,  Gravity.CENTER_VERTICAL, 100, 100);
		winOnScreen.window.update();  
	}
	
	static void dismiss(boolean isFinished) {
		if(winOnScreen != null) {
			winOnScreen.window.dismiss();
			if(isFinished && winOnScreen.finishListener!=null) {
				winOnScreen.finishListener.onFinish();
			}
			winOnScreen = null;
		}
	}
	
	static boolean isOnScreen() {
		return winOnScreen != null;
	}
	
	protected void setFinishListener(FinishListener finishListener) {
		this.finishListener = finishListener;
	}
}
