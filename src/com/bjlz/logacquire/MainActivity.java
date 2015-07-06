package com.bjlz.logacquire;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;


@SuppressLint({ "SimpleDateFormat", "HandlerLeak" }) public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initialize();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
	}


	@Override
	protected void onPause() {
		super.onPause();
	}

	
	private void initializeSpinner()
	{
		mSpinner = (Spinner) findViewById(R.id.log_spinner);
		BaseAdapter adapter = new BaseAdapter() {
			
			@Override
			public View getView(int pos, View v, ViewGroup vr) {
				LinearLayout ll = new LinearLayout(MainActivity.this);
				ll.setOrientation(LinearLayout.HORIZONTAL);
				TextView textview = new TextView(MainActivity.this);
				textview.setText(getItem(pos));
				textview.setTextSize(15);
				textview.setTextColor(Color.WHITE);
				ll.addView(textview);
				return ll;
			}
			
			@Override
			public long getItemId(int pos) {
				return pos;
			}
			
			@Override
			public String getItem(int pos) {
				return mCommand[pos];
			}
			
			@Override
			public int getCount() {
				return mCommand.length;
			}
		};
		if (mSpinner != null) {
			mSpinner.setAdapter(adapter);
			mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int pos, long arg3) {
					Log.d(TAG, "onItemSeleccted pos=" + pos + "arg3=" + arg3);
					mShellCmd = mCommand[pos];
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO 自动生成的方法存根
					
				}
			});
		}
	}

	private void initialize()
    {
    	mAcquireBtn = (Button) findViewById(R.id.log_acquire_btn);
    	if (mAcquireBtn != null) {
    		mAcquireBtn.setOnClickListener(mListener);
    	}
    	mPath = (TextView) findViewById(R.id.log_path_name);
    	mContent = (TextView) findViewById(R.id.log_content);
    	initializeSpinner();
    	boolean sdCardExist = Environment.getExternalStorageState().equals(  
                android.os.Environment.MEDIA_MOUNTED);
    	if (sdCardExist) {
    		Date date = new Date();
    		SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");
    		String path = simple.format(date);
    		
    		mPathName = Environment.getExternalStorageDirectory().toString()  
                    + File.separator + "logcat." + path + ".txt";
    		if (mPath != null) {
    			mPath.setText("path：" + mPathName);
    		} else {
    			mPath.setText("path:");
    		}
    	}
    }
    
    private void handleOnLogAcquire()
    {
    	//mShellCmd = "logcat -v time -s AndroidRuntime:E"; 
       	stratProcess(mShellCmd);
        try {
        	mInputStream = mCurProcess.getInputStream();
            File dir = null;  
            if (mPathName != null)  {
            	Log.d(TAG, "sd card exist, filePath:" + mPathName);  
                dir = new File(mPathName);
                if (!dir.exists()) {  
                    dir.createNewFile();  
                }  
            } else {
            	Log.e(TAG, "sdcard is not exist just return");
            	return;
            }
      
            try {  
            	mFos = new FileOutputStream(dir); 
                try {
                	int readLength = 1024;
                	mBytesLeft =  50 * 1024;
                    while (mBytesLeft > 0)  {
                        int read = mInputStream.read(mBuffer, 0, readLength);  
                        if (read == -1)  {
                        	Log.d(TAG, "read == -1 ");
                            throw new EOFException("Unexpected end of data");  
                        } 
                        Log.d(TAG, "readLength:" + readLength + ",read=" + read);
                        sendMsg(mBuffer, read);
                        mFos.write(mBuffer, 0, read);  
                        mBytesLeft -= readLength;  
                    }
                } finally  {
                	Log.d(TAG, "mFos finally");
                	if (mFos != null) {
                		mFos.close(); 
                	}
                }  
            } finally {
            	Log.d(TAG, "mInputStream finally");
            	if (mInputStream != null) {
            		mInputStream.close();  
            	}
            }
            Log.d(TAG, "LOGCAT = ok");  
            
        } catch (IOException e) {
             e.printStackTrace();  
        }  
    }
    
    private void stratProcess(String command)
    {
    	if (mInputStream != null) {
    		try {
				mInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		mInputStream = null;
    	}
    	if (mFos != null) {
    		try {
				mFos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		mFos = null;
    	}
    	
    	if (mCurProcess != null) {
    		mCurProcess.destroy();
    		mCurProcess = null;
    	}
        try {
			mCurProcess = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}  
           
    }
    
    private void sendMsg(byte[] value, int length)
    {
    	if (value == null || mHandler == null) {
    		return;
    	}
    	if (length > value.length) {
    		length = value.length;
    	}
    	String content = new String(value, 0, length);
    	Message msg = mHandler.obtainMessage();
    	msg.what = UPDATE_CONTENT;
    	msg.obj = content;
    	mHandler.sendMessage(msg);
    	
    }
    
    private Handler mHandler = new Handler()
    {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATE_CONTENT:
				String str = (String)msg.obj;
				if (mContent != null) {
					mContent.append(str);
				}
				break;
			default:
				
				break;
			}
		}
    	
    };
    
    private final int UPDATE_CONTENT = 0x12;
    private final String TAG = "LogAcquire";
    private String mPathName = null;
    private Button mAcquireBtn;
    private TextView mPath, mContent;
    private Process mCurProcess = null;
    private InputStream mInputStream;
    private FileOutputStream mFos;
    private Spinner mSpinner;
    private byte[] mBuffer = new byte[1024];  
    private int mBytesLeft;
    
    private String[] mCommand = new String[] {
    	"logcat -v time -s AndroidRuntime:E",
    	"logcat -v time -s DEBUG"
    };
    private String mShellCmd = mCommand[0];
    private View.OnClickListener mListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			if (mContent != null) {
				mContent.setText("");
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					handleOnLogAcquire();
				}
			}).start();
					
		}
	};
}
