package com.gavntery.GTReader;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowser extends Activity {

	private FileAdapter fileAdapter;
	private ListView listView_files;
	private File currDir;
	private FileComparator fileComp;
	private TextView textView_empty;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_browser);
		
		fileComp = new FileComparator();
		fileAdapter = new FileAdapter(this, R.layout.file_browser_list_item, new ArrayList<File>());
		findViews();
		listView_files.setLongClickable(true);
		setListeners();
		listView_files.setEmptyView(textView_empty);
		registerForContextMenu(listView_files);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		SharedPreferences setting = getSharedPreferences(getString(R.string.SETTING_FILENAME), 0);
		String currPath = setting.getString(getString(R.string.SETTING_FILEPATH), "/");
		
		currDir = new File(currPath);
		
		setCurrentDir(currDir);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		SharedPreferences setting = getSharedPreferences(getString(R.string.SETTING_FILENAME), 0);
		SharedPreferences.Editor editor = setting.edit();
		editor.putString(getString(R.string.SETTING_FILEPATH), currDir.getPath());
		editor.commit();
		super.onPause();
	}
	
	private void findViews()
	{
		listView_files = (ListView)findViewById(R.id.listView_filesList);
		textView_empty = (TextView)findViewById(R.id.textView_empty);
	}
	
	private void setListeners()
	{
		listView_files.setOnItemClickListener(fileItem_click_listener);
	}
	
	private ListView.OnItemClickListener fileItem_click_listener = new ListView.OnItemClickListener()
	{

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
			// TODO Auto-generated method stub
			File f = fileAdapter.getItem(position);
			
			if (!f.canRead())
			{
				return;
			}
			
			if (f.isFile())
			{
				openFile(f);
			}
			else if (f.isDirectory())
			{
				if (f.getName().endsWith(".."))
				{
					currDir = currDir.getParentFile();
					setCurrentDir(currDir);
				}
				else
				{
					currDir = f;
					setCurrentDir(currDir);
				}
			}
		}

	};
	
	static class FileAdapter extends ArrayAdapter<File>{
		private LayoutInflater inflater;
		
		private int colorWhite;
		private int colorGray;
		
		private Bitmap iconFolder;
		private Bitmap iconFolderUnreadable;
		private Bitmap iconTextGeneric;
		
		public FileAdapter(Context context, int textViewResourceId, List<File> files) {
			super(context, textViewResourceId, files);
			
			inflater = LayoutInflater.from(context);
			
			iconFolder = BitmapFactory.decodeResource(context.getResources(), R.drawable.folder);
			iconFolderUnreadable = BitmapFactory.decodeResource(context.getResources(), R.drawable.folder_unreadable);
			iconTextGeneric = BitmapFactory.decodeResource(context.getResources(), R.drawable.text_x_generic);
			
			context.getResources().getColor(R.color.black);
			colorWhite = context.getResources().getColor(R.color.white);
			colorGray = context.getResources().getColor(R.color.gray);
		}
		
		
		private Bitmap getBitmap(File f)
		{
			if (f.isDirectory())
			{
				if (!f.canRead())
				{
					return iconFolderUnreadable;
				}
				
				return iconFolder;
			}
			else
			{
				return iconTextGeneric;
			}
		}
		
		private int getColor(File f)
		{
			
			if (f.isFile() && !f.canRead() || f.isDirectory() && !f.canWrite())
			{
				return colorGray;
			}
			else
			{
				return colorWhite;
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.file_browser_list_item, null);
				
				holder = new ViewHolder();
				holder.imageView_fileIcon = (ImageView)convertView.findViewById(R.id.imageView_fileIcon);
				holder.textView_fileName = (TextView)convertView.findViewById(R.id.textView_fileName);
				holder.textView_fileDesc = (TextView)convertView.findViewById(R.id.textView_fileDesc);
				
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder)convertView.getTag();
			}
			
			File f = getItem(position);
			
			holder.file = f;
			
			holder.textView_fileName.setText(f.getName());
			holder.textView_fileName.setTextColor(getColor(f));
			
			holder.textView_fileDesc.setText(formatDesc(f));
			
			holder.imageView_fileIcon.setImageBitmap(getBitmap(f));
			
			return convertView;
		}
		
	}
	
	static class ViewHolder {
		ImageView imageView_fileIcon;
		TextView textView_fileName;
		TextView textView_fileDesc;
		
		File file;
	}
	
	static String formatDesc(File f)

	{
		if (f.getName().equals(".."))
		{
			return "To parent directory";
		}
		
		if (f.isDirectory())
		{
			int fNum = countFiles(f);
			if (fNum < 0)
			{
				return "Unreadable";
			}
			else
			{
				return String.format("%d item%s", fNum, fNum != 1 ? "s" : "");
			}
		}
		else
		{
			return formatByteSize(f.length());
		}
	}
	
	/**
	 * Counts the number of files in the directory given by f.
	 * @param f the given directory
	 * @return
	 */
	static int countFiles(File f)
	{
		if (!f.isDirectory()) return -1;
		File [] files = f.listFiles();
		if (files == null) return -2;
		return files.length;
	}
	
	static String formatByteSize(long sz)
	{
		DecimalFormat df = new DecimalFormat("#,###.#");
		if (sz > 10000000) return String.format("%s M", df.format(sz/1000000.0));
		if (sz > 10000) return String.format("%s K", df.format(sz/1000.0));
		return String.format("%s B", df.format(sz));
	}
	
	private void openFile(File f)
	{
		if (!f.canRead())
		{
			return;
		}
		
		String fname = f.getName();
		if (!fname.toLowerCase().endsWith(".txt"))
		{
			return;
		}
		
		try 
		{
			Intent intent = new Intent();
			intent.setClass(FileBrowser.this, TxtViewer.class);
			intent.putExtra(getString(R.string.SETTING_FILEPATH), f.getCanonicalPath());
			
			startActivity(intent);
		} catch (IOException ie)
		{
			
		}
		
	}
	
	private void setCurrentDir(File dir)
	{
		if (!dir.isDirectory())
		{
			return;
		}
		fileAdapter.clear();
		
		File [] files = dir.listFiles();
		
		Arrays.sort(files, fileComp);
		
		// the 'up a directory' file
		if (!dir.getName().equals(""))
		{
			fileAdapter.add(new File(".."));
		}
		for (File f: files)
		{
			fileAdapter.add(f);
		}
		listView_files.setAdapter(fileAdapter);
		
		try {
			setTitle(dir.getCanonicalPath() + " (" + formatDesc(dir) + ")");
		} catch (IOException e)
		{
			setTitle(dir.getAbsolutePath() + " (" + formatDesc(dir) + ")");
		}
	}
	
	static class FileComparator implements Comparator<File>
	{
		@Override
		public int compare(File f1, File f2)
		{
			if (f1.isDirectory() && !f2.isDirectory())
			{
				return -1;
			}
			else if (!f1.isDirectory() && f2.isDirectory())
			{
				return 1;
				
			}
			else
			{
				String f1Name = f1.getName();
				String f2Name = f2.getName();
				return f1Name.compareToIgnoreCase(f2Name);
			}
		}
	}

}
