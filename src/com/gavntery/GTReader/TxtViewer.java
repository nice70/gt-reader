package com.gavntery.GTReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class TxtViewer extends Activity implements OnGestureListener {
	
	//private String SDPATH = Environment.getExternalStorageDirectory() + "/";
	
	String fileName;
	private TextView textView_reader;
	private GestureDetector mGestureDetector;
	private Paint mPaint = null;
	
	private static final int FLING_MIN_DISTANCE = 100;
	private static final int FLING_MIN_VELOCITY = 200;
	private int screenWidth =0;
	private int screenHeight =0;
	private int MaxBytesPerPage=0;
	private int NumCharInRow=0;
	private int NumCharInColum=0;
	private int CurrentByteInPage=0;
	private long TotalSkipBytes=0;//the number of bytes to skip to load the next page
	//
	private int Setting_text_size=0;
	private String Setting_text_code;
	private int Setting_text_color;
	private int Setting_bg_color;
	private int ReadBytes=0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		//Start of hiding status and title bar
		final Window win = getWindow();
		//Hiding Status Bar
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//Hiding Title Bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//End of hiding status and title bar
		
		setContentView(R.layout.viewer);
		getFileName();
		textView_reader = (TextView)findViewById(R.id.textView_reader);
		mGestureDetector = new GestureDetector(this);
		
		/*
		 * since the dimension function can not get in OnCreate method, 
		 * so move to a new Runnable with run() method
		*/
		textView_reader.post(new Runnable(){
			@Override
			public void run() {
				SetTextField();
			}
		});
	}
	
	private void getFileName()
	{
		Intent intent=getIntent();
		fileName=intent.getStringExtra(getString(R.string.SETTING_FILEPATH));
	}
	
	//首次打开text，根据设置和屏幕尺寸显示文本
	public void SetTextField()
	{
		//first load the current settings include the size of text set
		LoadSettings();
		GetDimensionOfView();
		
		textView_reader.setTextSize(Setting_text_size);
		System.out.println("textView_reader TextSize = " + textView_reader.getTextSize());
		System.out.println("Setting_text_size = " + Setting_text_size);
		textView_reader.setTextColor(DefaultSetting.color[Setting_text_color]);
		textView_reader.setBackgroundColor(DefaultSetting.color[Setting_bg_color]);
		textView_reader.setText(getStringFromFileForward(TotalSkipBytes));
	}
	
	private void LoadSettings()
	{
        SharedPreferences settings = getSharedPreferences(getString(R.string.SETTING_FILENAME), 0);
        GetTextCode();
        Resources res = getResources();
        Setting_text_size = settings.getInt(getString(R.string.SETTING_TEXTSIZE), res.getInteger(R.integer.DefTextSize));
    	Setting_text_color= settings.getInt(getString(R.string.SETTING_TEXT_COLOR), res.getInteger(R.integer.DefTextColor));
    	Setting_bg_color= settings.getInt(getString(R.string.SETTING_BG_COLOR), res.getInteger(R.integer.DefBgColor));
    	TotalSkipBytes = settings.getLong(getString(R.string.SETTING_BOOKSPAGENUM), 0);
	}
	
	private void GetTextCode()
	{
		try{
			FileInputStream fileIS = new FileInputStream(fileName);
			BufferedInputStream buf = new BufferedInputStream(fileIS);
			buf.mark(4);
			byte[] first3bytes = new byte[3];
			buf.read(first3bytes);//找到文档的前三个字节并自动判断文档类型
			buf.reset();
			if(first3bytes[0] == (byte)0xEF && first3bytes[1] == (byte)0xBB && first3bytes[2] == (byte)0xBF) {
				Setting_text_code = "utf-8";
			}else if(first3bytes[0] == (byte)0xFF && first3bytes[1] == (byte)0xFE) {
				Setting_text_code = "unicode";
			}else if(first3bytes[0] == (byte)0xFE && first3bytes[1] == (byte)0xFF) {
				Setting_text_code = "utf-16be";
			}else if(first3bytes[0] == (byte)0xFF && first3bytes[1] == (byte)0xFF) {
				Setting_text_code = "utf-16le";
			}else {
				Setting_text_code = "GBK";
			}
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Calculate the actual size of available of text view
	 * ScreenHeight-titleBarWeight-StatusBarWeight
	 * to get these values they must not run in OnCreate() method as the layout has not shown yet
	 */
	private void GetDimensionOfView()
	{
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels;
//		Rect frame = new Rect();
//		getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
//		int statusBarHeight = frame.top;
//		int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
//		//statusBarHeight是上面所求的状态栏的高度
//		int titleBarHeight = contentTop - statusBarHeight;
//		screenHeight=screenHeight-(titleBarHeight+statusBarHeight);
		MaxBytesPerPage= 2 * MaxBytesPerPage();
	}
	
	/*
	 * Calculate how many bytes can be shown on the screen
	 */
	private int MaxBytesPerPage()
	{
		int totalbyte;
		NumCharInRow=screenWidth/Setting_text_size;
		NumCharInColum=screenHeight/Setting_text_size;
		//Assume a space between lines so minus the max lines*the space
		//This method still need to improve this is only a assumption and in some condition it perform badly
		NumCharInColum=(int) ((screenHeight-NumCharInColum*DefaultSetting.LineSpace)/Setting_text_size);
		//Temp method to reduce the effect on some special case that the texts are covered by the bottom of the screen
		totalbyte=NumCharInRow*(NumCharInColum-1);
		return totalbyte;
		
	}
	
	/*
	 * The actual function to load the txt file in forward mode
	 */
	public String getStringFromFileForward(long pagenumber)
	{
		char buff[]=new char[MaxBytesPerPage];
		try {
			//First open a file if not exit throw an exception
			String sBuffer = null;
			FileInputStream fInputStream = new FileInputStream(fileName);
			//Put the file into a buffer stream
			InputStreamReader inputStreamReader = new InputStreamReader(fInputStream, Setting_text_code);
			BufferedReader in = new BufferedReader(inputStreamReader);
			in.skip(pagenumber);
			ReadBytes=in.read(buff,0,MaxBytesPerPage);
			in.close();
			calculateBytesNeed(buff);
			sBuffer=new String(buff,0,CurrentByteInPage);
			return sBuffer;
		} catch (Exception e) {
			//System.out.println("Exception happend");
			e.printStackTrace();	
		}
		return null;
	}
	
	/*
	 * Load the txt words when flipper to right
	 */
	public String getStringFromFileBackwards(long pagenumber)
	{
		char buff[]=new char[MaxBytesPerPage];
		try {
			String sBuffer = null;
			FileInputStream fInputStream = new FileInputStream(fileName);
			InputStreamReader inputStreamReader = new InputStreamReader(fInputStream, Setting_text_code);
			BufferedReader in = new BufferedReader(inputStreamReader);
			if(!new File(fileName).exists())
			{
				return null;
			}
			//include itself so plus one to let the buff contains char at the position of page number 
			pagenumber=pagenumber-MaxBytesPerPage;
			if(pagenumber<0)
			{
				pagenumber=0;
			}
			in.skip(pagenumber);
			//in.read(buff,0,numByte);
			ReadBytes=in.read(buff,0,MaxBytesPerPage);
			if(pagenumber==0)
			{
				calculateBytesNeed(buff);
				sBuffer=new String(buff,0,CurrentByteInPage);
			}
			else
			{
				calculateBytesNeedBackwards(buff);
				sBuffer=new String(buff,MaxBytesPerPage-CurrentByteInPage,CurrentByteInPage);
			}
			
			in.close();
			return sBuffer;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//Record skip how many bytes, so it is like page number
    private void updatePageNum()
    {
    	SharedPreferences settings = getSharedPreferences(getString(R.string.SETTING_FILENAME), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(getString(R.string.SETTING_BOOKSPAGENUM), TotalSkipBytes);
        editor.commit();
    }
	
	private void calculateBytesNeed(char[] data)
	{
		int totalLines=0;
		int newLineCharTotalWidth=0;
//		int isStart = 0;
		int i=0;
		mPaint = textView_reader.getPaint();
		System.out.println("textView_reader TextSize2 = " + textView_reader.getTextSize());
		System.out.println("textSize = " + mPaint.getTextSize());
		
		FontMetrics fm = mPaint.getFontMetrics();
		int m_iFontHeight = (int) Math.ceil(fm.descent - fm.top);//字体高度
		System.out.println("fm.ascent = " + fm.ascent);
		System.out.println("fm.descent = " + fm.descent);
		System.out.println("fm.bottom = " + fm.bottom);
		System.out.println("fm.top = " + fm.top);
		System.out.println("m_iFontHeight = " + m_iFontHeight);
		System.out.println("MaxBytesPerPage = " + MaxBytesPerPage);
		System.out.println("screenHeight = " + screenHeight);
		System.out.println("screenWidth = " + screenWidth);
		
		for(i=0;i<MaxBytesPerPage;i++)
		{
			//NewLineBytes++;
			float[] char_width = new float[1];
			String str = String.valueOf(data[i]);
			mPaint.getTextWidths(str, char_width);
			
//			if(NewLineBytes%NumCharInRow==0)
//			{
//				TotalLines++;
//			}
			if(data[i]=='\n')
			{
				newLineCharTotalWidth=0;
//				isStart = i + 1;
				totalLines++;
			}
			else
			{
				newLineCharTotalWidth += (int)(Math.ceil(char_width[0]));
				if(newLineCharTotalWidth > screenWidth) 
				{
					totalLines++;
//					isStart = i;
					i--;
					newLineCharTotalWidth = 0;
				}
				else
				{
					if(i == data.length)
					{
						totalLines++;
					}
				}
			}
			if((m_iFontHeight * totalLines) > screenHeight) 
			{
				CurrentByteInPage=i+1;
				System.out.println("totalLines(return) = " + totalLines);
				System.out.println("CurrentByteInPage(return)  = " + CurrentByteInPage);
				return;
			}
//			if(totalLines>=NumCharInColum)
//			{
//				//System.out.println("break Line number is "+TotalLines);
//				break;
//			}
		}	
		CurrentByteInPage=i+1;
		System.out.println("totalLines = " + totalLines);
		System.out.println("CurrentByteInPage = " + CurrentByteInPage);
	}
	
	private void calculateBytesNeedBackwards(char[] data)
	{
		int totalLines=0;
		int newLineCharTotalWidth=0;
		int i=0;
		mPaint = textView_reader.getPaint();
		FontMetrics fm = mPaint.getFontMetrics();
		int m_iFontHeight = (int) Math.ceil(fm.descent - fm.top) + 4;//字体高度
		
		for(i=MaxBytesPerPage-1;i>=0;i--)
		{
			float[] char_width = new float[1];
			String str = String.valueOf(data[i]);
			mPaint.getTextWidths(str, char_width);
//			if(NewLineBytes%NumCharInRow==0)
//			{
//				TotalLines++;
//			}
			if(data[i]=='\n')
			{
				newLineCharTotalWidth=0;
				totalLines++;
			}
			else
			{
				newLineCharTotalWidth += (int)(Math.ceil(char_width[0]));
				if(newLineCharTotalWidth > screenWidth) 
				{
					totalLines++;
//					isStart = i;
					i--;
					newLineCharTotalWidth = 0;
				}
				else
				{
					if(i == data.length)
					{
						totalLines++;
					}
				}
			}
			if((m_iFontHeight * totalLines) > screenHeight) 
			{
				CurrentByteInPage=MaxBytesPerPage-i;
				return;
			}
//			if(totalLines>=NumCharInColum)
//			{
//				//System.out.println("break Line number is "+TotalLines);
//				break;
//			}
		}	
		//翻页之后的
		CurrentByteInPage=MaxBytesPerPage-i;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 一定要将触屏事件交给手势识别类去处理（自己处理会很麻烦的）
		return mGestureDetector.onTouchEvent(event);
		
	}
	
	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenWidth = dm.widthPixels;
		//当点击屏幕左侧
		if(arg0.getX()<(screenWidth/2))
		{
			textView_reader.setText(getStringFromFileBackwards(TotalSkipBytes));
			TotalSkipBytes=TotalSkipBytes-CurrentByteInPage;
			if(TotalSkipBytes<0)
				TotalSkipBytes=0;
			updatePageNum();
			
		}
		//当点击屏幕右侧
		else if(arg0.getX()>(screenWidth/2))
		{
			if(ReadBytes!=-1)
			{
				TotalSkipBytes=TotalSkipBytes+CurrentByteInPage;
				textView_reader.setText(getStringFromFileForward(TotalSkipBytes));
				updatePageNum();
			}
		}
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
				&& Math.abs(velocityX) > FLING_MIN_VELOCITY) {
			// 当像左侧滑动的时候 //设置View进入屏幕时候使用的动画
			//mFlipper.setInAnimation(inFromRightAnimation());
			// 设置View退出屏幕时候使用的动画
		//mFlipper.setOutAnimation(outToLeftAnimation());
			//mFlipper.showNext();
			if(ReadBytes!=-1)
			{
				TotalSkipBytes=TotalSkipBytes+CurrentByteInPage;
				textView_reader.setText(getStringFromFileForward(TotalSkipBytes));
				updatePageNum();
				//System.out.println("CurrentByteInPage is "+CurrentByteInPage);
				//System.out.println("skipnumber is in getStringFromFile"+TotalSkipBytes);
				//System.out.println("**************Slide to left************");
			}
		} else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
				&& Math.abs(velocityX) > FLING_MIN_VELOCITY) {

			// 当像右侧滑动的时候
		//	mFlipper.setInAnimation(inFromLeftAnimation());
			//mFlipper.setOutAnimation(outToRightAnimation());
		//	mFlipper.showPrevious();
			textView_reader.setText(getStringFromFileBackwards(TotalSkipBytes));
			TotalSkipBytes=TotalSkipBytes-CurrentByteInPage;
			if(TotalSkipBytes<0)
				TotalSkipBytes=0;
			updatePageNum();
			//System.out.println("CurrentByteInPage is "+CurrentByteInPage);
			//System.out.println("skipnumber is in getStringFromFile"+TotalSkipBytes);
			//System.out.println("**************Slide to right************");
		}
		
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}
}
