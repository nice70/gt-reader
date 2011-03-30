package com.gavntery.GTReader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.EditText;

public class ViewerSetting extends Activity {
	
	private int setting_text_size;
	private int setting_text_color;
	private int setting_bg_color;
	
	EditText editView_textSize;
	EditText editView_textColor;
	EditText editView_viewerBGColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		LoadSettings();
		setContentView(R.layout.viewer_setting);
		FindView();
		SetView();
		super.onCreate(savedInstanceState);
	}
	
	private void LoadSettings() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.SETTING_FILENAME), 0);
        Resources res = getResources();
        setting_text_size = settings.getInt(getString(R.string.SETTING_TEXTSIZE), res.getInteger(R.integer.DefTextSize));
    	setting_text_color= settings.getInt(getString(R.string.SETTING_TEXT_COLOR), res.getInteger(R.integer.DefTextColor));
    	setting_bg_color= settings.getInt(getString(R.string.SETTING_BG_COLOR), res.getInteger(R.integer.DefBgColor));
	}
	
	private void FindView() {
		editView_textSize = (EditText)findViewById(R.id.editText_testSize);
		editView_textColor = (EditText)findViewById(R.id.editText_textColor);
		editView_viewerBGColor = (EditText)findViewById(R.id.editText_viewerBGColor);
	}
	
	private void SetView() {
		editView_textSize.setText(setting_text_size);
		editView_textColor.setText(setting_text_color);
		editView_viewerBGColor.setText(setting_bg_color);
	}

}
