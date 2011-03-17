package com.gavntery.GTReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class main extends Activity {
	private String SDPATH = Environment.getExternalStorageDirectory() + "/";
	
	String test_text = SDPATH + "book/test.txt";
	private TextView textView_reader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		textView_reader = (TextView)findViewById(R.id.textView_reader);
		String file_text = read_file(test_text);
		textView_reader.setText(file_text);
	}
	
	private String read_file(String file_addr) {
		BufferedReader reader;
		String readString = new String();
		
		try {
			FileInputStream fileIS = new FileInputStream(file_addr);
			BufferedInputStream buf = new BufferedInputStream(fileIS);
			buf.mark(4);
			byte[] first3bytes = new byte[3];
			buf.read(first3bytes);//找到文档的前三个字节并自动判断文档类型
			buf.reset();
			if(first3bytes[0] == (byte)0xEF && first3bytes[1] == (byte)0xBB && first3bytes[2] == (byte)0xBF) {
				reader = new BufferedReader(new InputStreamReader(buf, "utf-8"));
			}else if(first3bytes[0] == (byte)0xFF && first3bytes[1] == (byte)0xFE) {
				reader = new BufferedReader(new InputStreamReader(buf, "unicode"));
			}else if(first3bytes[0] == (byte)0xFE && first3bytes[1] == (byte)0xFF) {
				reader = new BufferedReader(new InputStreamReader(buf, "utf-16be"));
			}else if(first3bytes[0] == (byte)0xFF && first3bytes[1] == (byte)0xFF) {
				reader = new BufferedReader(new InputStreamReader(buf, "utf-16le"));
			}else {
				reader = new BufferedReader(new InputStreamReader(buf, "GBK"));
			}
			String readLine = new String();
			while((readLine = reader.readLine()) != null) {
				Log.d("line:", readLine);
				readString += readLine + "\n";
			}
			fileIS.close();
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
		return readString;
	}

}
