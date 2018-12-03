package com.syscan.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.syscan.gm930.BarcodeReader;
import com.syscan.scandemo.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;

import jxl.write.DateTime;

public class MainActivity extends Activity{

	private BarcodeReader mBarcodeReader;

	private Handler mMainHandler;
	private int flag=0;
	private TextView mTextview,mtitle;
	SpannableStringBuilder spanBuilder=null;
	private EditText aid;
	private Button get_qrcode,set,reset;
	private BeepManager mBeepManager;  //媒体播放者，用于播放提示音
	private String atitle="";
	private int m_scan_num=0;
	private SharedPreferences aidSettings =null;
	private SharedPreferences.Editor editor = null;
	private Vibrator vibrator =null;
	private static final String TAG = "MainActivity";  //Debug

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Vibrator vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);
		aidSettings = getSharedPreferences("setting", 0);
		editor = aidSettings.edit();
		isNetWorkAvailable();
		//初始化
		init();
	}

	@SuppressLint("HandlerLeak")
	private void init(){

		mTextview = (TextView) findViewById(R.id.text_view);
		mtitle=(TextView) findViewById(R.id.title);
		aid=(EditText) findViewById(R.id.aid);
		mBeepManager = new BeepManager(this, R.raw.beep);
		set=(Button) findViewById(R.id.set);
		reset=(Button) findViewById(R.id.reset);
		get_qrcode=(Button) findViewById(R.id.get_qrcode);
		getaid();
		mMainHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ");
				//String date=sDateFormat.format(new java.util.Date());
				// 接收子线程的消息

				switch (BarcodeReader.MESSAGE_TYPE.values()[msg.what])
				{
					case MSG_BARCODE://解码
						//byte[] data = (byte[]) msg.obj;
						String str=(String)msg.obj;
//						try {
//							str = new String(data, "GB18030");
//						} catch (UnsupportedEncodingException e) {
//							e.printStackTrace();
//							str = "Barcode to String failed!";
//						}
						m_scan_num++;
						// mtitle.setText(date+m_scan_num+"\n");
						System.out.println("haha "+str);


						solve(str);
						break;
					default:
						break;
				}
				super.handleMessage(msg);
			}
		};

		mBarcodeReader = new BarcodeReader();
	}





	@Override
	protected void onPause() {
		mBarcodeReader.close();
		Log.w(TAG, "pause close reader");

		super.onPause();
	}

	@Override
	protected void onResume() {
		mBarcodeReader.open(mMainHandler);
		Log.w(TAG, "resume open reader");
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		mBarcodeReader.close();
		Log.w(TAG, "destroy close reader");
		super.onDestroy();
		System.exit(0);
	}


	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.set:
				if(!isNetWorkAvailable())
					break;
				if(aid.getText().length()>0)
				{
					if(check(aid.getText().toString()))
					{
						String aid1=aid.getText().toString();
						long endtime=Long.parseLong(getendtime(aid1))+43200,currenttime=System.currentTimeMillis()/1000;
						if(endtime!=43200&&endtime<currenttime)
						{
							Toast toast = Toast.makeText(MainActivity.this, "活动已过期", Toast.LENGTH_SHORT);
							toast.show();
							mBeepManager.playBeepSound();
							try {
								Thread.sleep(300);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mBeepManager.playBeepSound();
						}
						else
						{
							atitle=gettitle(aid1);

							//高亮标题
							ColorStateList redColors = ColorStateList.valueOf(0xffff0000);
							spanBuilder = new SpannableStringBuilder(atitle+"\n");
							spanBuilder.setSpan(new TextAppearanceSpan(null, 0, 60, redColors, null), 0, atitle.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

							mtitle.setText(spanBuilder);
							editor.putString("aid",aid.getText().toString());
							editor.putString("atitle",atitle);
							editor.commit();
							aid.setEnabled(false);
							set.setEnabled(false);
							get_qrcode.setEnabled(false);
							Toast toast = Toast.makeText(MainActivity.this, "设定成功", Toast.LENGTH_SHORT);
							toast.show();
							mBeepManager.playBeepSound();
						}
					}
					else
					{
						Toast toast = Toast.makeText(MainActivity.this, "无该活动，设定失败", Toast.LENGTH_SHORT);
						toast.show();
						mBeepManager.playBeepSound();
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mBeepManager.playBeepSound();
					
						
						//vibrator.vibrate(1000);
					}
				}
				else
				{
					Toast toast = Toast.makeText(MainActivity.this, "请输入活动id", Toast.LENGTH_SHORT);
					toast.show();
				}
				break;
			case R.id.single_scan:
				if(!isNetWorkAvailable())
					break;
				if(aid.isEnabled()==true)
				{
					new AlertDialog.Builder(this)
							.setTitle("提示")
							.setMessage("请设定活动id或活动标题")
							.setPositiveButton("确定", null)
							.show();
				}
				else
				{
					mBarcodeReader.single_scan(3000);
					flag=1;
				}
				break;
			case R.id.continuous_scan:
				if(!isNetWorkAvailable())
					break;
				if(aid.isEnabled()==true)
				{
					new AlertDialog.Builder(this)
							.setTitle("提示")
							.setMessage("请设定活动id或活动标题")
							.setPositiveButton("确定", null)
							.show();
				}
				else {
					mBarcodeReader.continuous_scan();
					flag=1;
				}
				break;
			case R.id.stop_btn:
				mBarcodeReader.stop_scan();
				break;
			case R.id.reset:
				aid.setText("");
				mtitle.setText("");
				mTextview.setText("");
				aid.setEnabled(true);
				set.setEnabled(true);
				get_qrcode.setEnabled(true);
				editor.remove("aid");
				editor.remove("atitle");
				editor.commit();
				flag=0;
				break;
			case R.id.exit_btn:
				mBarcodeReader.close();
				Log.w(TAG, "exit close reader");
				finish();
				//强制退出app，以免占用串口导致其他app不能正常运行
				System.exit(0);
				break;
			case R.id.clear_btn:
				mTextview.setText("");
				//aid.setText("");
				//aid.setEnabled(true);
				// set.setEnabled(true);
				//get_qrcode.setEnabled(true);
				break;
			case R.id.get_qrcode:
				if(!isNetWorkAvailable())
					break;
				mBarcodeReader.single_scan(3000);
				break;
			default:
				break;
		}
	}


	public void getaid()
	{
		String id = aidSettings.getString("aid","默认值");
		String atitle = aidSettings.getString("atitle","默认值");

		//高亮标题
		ColorStateList redColors = ColorStateList.valueOf(0xffff0000);
		spanBuilder = new SpannableStringBuilder(atitle+"\n");
		spanBuilder.setSpan(new TextAppearanceSpan(null, 0, 60, redColors, null), 0, atitle.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

		if(check(id))
		{
			aid.setText(id);
			aid.setEnabled(false);
			set.setEnabled(false);
			get_qrcode.setEnabled(false);
			aid.setEnabled(false);
			mtitle.setText(spanBuilder);
			Toast toast = Toast.makeText(MainActivity.this, "活动id设定成功", Toast.LENGTH_SHORT);
			toast.show();
			mBeepManager.playBeepSound();
		}

	}
	public void solve(String str)
	{
		if(str.contains("/")&&flag==1)
		{
			Toast toast = Toast.makeText(MainActivity.this, "请扫用户二维码", Toast.LENGTH_SHORT);
			toast.show();
			mBeepManager.playBeepSound();
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mBeepManager.playBeepSound();
		}
		else if(!str.contains("/") &&flag==0)
		{
			Toast toast = Toast.makeText(MainActivity.this, "请扫活动二维码", Toast.LENGTH_SHORT);
			toast.show();
			mBeepManager.playBeepSound();
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mBeepManager.playBeepSound();
		}
		else if(str.contains("/") &&flag==0)
		{
			String regEx = "[^0-9]";
			Pattern p = Pattern.compile(regEx);
			// 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
			Matcher m = p.matcher(str);

			//将输入的字符串中非数字部分用空格取代并存入一个字符串
			String string = m.replaceAll(" ").trim();

			//以空格为分割符在讲数字存入一个字符串数组中
			String[] strArr = string.split(" ");
			String aid1=strArr[strArr.length-1];

			if(check(aid1))
			{
				long endtime=Long.parseLong(getendtime(aid1))+43200,currenttime=System.currentTimeMillis()/1000;
				if(endtime!=43200&&endtime<currenttime)
				{
					Toast toast = Toast.makeText(MainActivity.this, "活动已过期", Toast.LENGTH_SHORT);
					toast.show();
					mBeepManager.playBeepSound();
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mBeepManager.playBeepSound();
				}
				else
				{
					atitle=gettitle(aid1);

					//高亮标题
					ColorStateList redColors = ColorStateList.valueOf(0xffff0000);
					spanBuilder = new SpannableStringBuilder(atitle+"\n");
					spanBuilder.setSpan(new TextAppearanceSpan(null, 0, 60, redColors, null), 0, atitle.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
					mtitle.setText(spanBuilder);

					editor.putString("aid",aid1);
					editor.putString("atitle",atitle);
					editor.commit();
					aid.setText(aid1);
					aid.setEnabled(false);
					set.setEnabled(false);
					get_qrcode.setEnabled(false);
					Toast toast = Toast.makeText(MainActivity.this, "设定成功", Toast.LENGTH_SHORT);
					toast.show();
					mBeepManager.playBeepSound();
				}
			}
			else
			{
				Toast toast = Toast.makeText(MainActivity.this, "无该活动，设定失败", Toast.LENGTH_SHORT);
				toast.show();
				mBeepManager.playBeepSound();
				mBeepManager.playBeepSound();
				mBeepManager.playBeepSound();
				//vibrator.vibrate(1000);
			}
		}
		else
		{
			if (aid.isEnabled() == false) {
				long currenttime = System.currentTimeMillis() / 1000;
				String s[] = str.split(" ");
				long lasttime = Long.parseLong(s[1]);
				if (currenttime - lasttime <= 5) {
					StringBuilder builder = new StringBuilder();
					String url = "http://ccnu.chunkao.cn/Devbranch/zcl/signin/signin.php";
					//showtips(s[0]+" "+aid.getText().toString());
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					if (aid.getText().length() > 0) {
						params.add(new BasicNameValuePair("aid", aid.getText().toString()));
						params.add(new BasicNameValuePair("uid", s[0]));
						params.add(new BasicNameValuePair("tag", "1"));
					} else {
						params.add(new BasicNameValuePair("uid", s[0]));
						params.add(new BasicNameValuePair("tag", "0"));
					}

					try {
						builder = getResponse(url, params);
						String num = builder.toString().trim();
						//showtips(num);
						String regEx = "[^0-9]";
						Pattern p = Pattern.compile(regEx);
						// 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
						Matcher m = p.matcher(num);

						//将输入的字符串中非数字部分用空格取代并存入一个字符串
						String string = m.replaceAll(" ").trim();

						//以空格为分割符在讲数字存入一个字符串数组中
						String[] strArr = string.split(" ");
						System.out.println(num);
						//检查用户是否已经报名该活动
						String studentid = getstudentid(s[0]);
						if (Integer.valueOf(strArr[0]) == 1) {
							//检查用户是否已经签到
							if (!checksignin(s[0])) {
								if (refreshsignin(s[0])) {
									Toast toast = Toast.makeText(MainActivity.this, "用户" + studentid + "认证成功", Toast.LENGTH_SHORT);
									toast.show();
									showMessage(s[0]);
									mBeepManager.playBeepSound();
								} else {
									Toast toast = Toast.makeText(MainActivity.this, "用户" + studentid + "认证成功但签到失败", Toast.LENGTH_SHORT);
									toast.show();

									mBeepManager.playBeepSound();
									Thread.sleep(300);
									mBeepManager.playBeepSound();
									//vibrator.vibrate(1000);
								}
							} else {
								Toast toast = Toast.makeText(MainActivity.this, "用户" + studentid + "已签到", Toast.LENGTH_SHORT);
								toast.show();
								mBeepManager.playBeepSound();
								Thread.sleep(300);
								mBeepManager.playBeepSound();
								//vibrator.vibrate(1000);
							}
						} else {

							Toast toast = Toast.makeText(MainActivity.this, "用户" + studentid + "认证失败", Toast.LENGTH_SHORT);
							toast.show();
							mBeepManager.playBeepSound();
							Thread.sleep(300);
							mBeepManager.playBeepSound();

							//vibrator.vibrate(1000);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else
				{
					Toast toast = Toast.makeText(MainActivity.this, "用户二维码已过期，请刷新页面", Toast.LENGTH_SHORT);
					toast.show();
				}
			}

		}
	}
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	public boolean check(String id)
	{
		StringBuilder builder = new StringBuilder();
		String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/check.php";
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("aid",id));
		try {
			builder=getResponse(url,params);
			String num = builder.toString().trim();

			System.out.println("num="+num);
			String regEx = "[^0-9]";
			Pattern p = Pattern.compile(regEx);
			// 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
			Matcher m = p.matcher(num);

			//将输入的字符串中非数字部分用空格取代并存入一个字符串
			String string = m.replaceAll(" ").trim();

			//以空格为分割符在讲数字存入一个字符串数组中
			String[] strArr = string.split(" ");

			System.out.println(num.length());
			if (Integer.valueOf(strArr[0])==1) {

				return true;
			} else {

				return false;

			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	public boolean checksignin(String uid)
	{
		StringBuilder builder = new StringBuilder();
		String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/checksignin.php";
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("aid",aid.getText().toString()));
		params.add(new BasicNameValuePair("uid", uid));



		try {
			builder=getResponse(url,params);
			String num = builder.toString().trim();
			System.out.println("num="+num);
			String regEx = "[^0-9]";
			Pattern p = Pattern.compile(regEx);
			// 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
			Matcher m = p.matcher(num);

			//将输入的字符串中非数字部分用空格取代并存入一个字符串
			String string = m.replaceAll(" ").trim();

			//以空格为分割符在讲数字存入一个字符串数组中
			String[] strArr = string.split(" ");

			System.out.println(num.length());
			if (Integer.valueOf(strArr[0])==1) {
				return true;
			} else {

				return false;

			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}


	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	public boolean refreshsignin(String uid)
	{
		StringBuilder builder = new StringBuilder();
		String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/refreshsignin.php";
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		params.add(new BasicNameValuePair("aid",aid.getText().toString()));
		params.add(new BasicNameValuePair("uid", uid));



		try {
			builder=getResponse(url,params);
			String num = builder.toString().trim();
			System.out.println("num="+num);
			String regEx = "[^0-9]";
			Pattern p = Pattern.compile(regEx);
			// 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
			Matcher m = p.matcher(num);

			//将输入的字符串中非数字部分用空格取代并存入一个字符串
			String string = m.replaceAll(" ").trim();

			//以空格为分割符在讲数字存入一个字符串数组中
			String[] strArr = string.split(" ");

			System.out.println(num.length());
			if (Integer.valueOf(strArr[0])==1) {
				return true;
			} else {

				return false;

			}

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	public void showtips(String uid)
	{
		new AlertDialog.Builder(this)
				.setTitle("提示")
				.setMessage(uid)
				.setPositiveButton("确定", null)
				.show();
	}

	public void showMessage(String uid)
	{

		try {
			StringBuilder builder = new StringBuilder();
			String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/showmessage.php";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("uid",uid));
			builder=getResponse(url,params);
			JSONArray jsonArray = new JSONArray(builder.toString());
			String studentid = jsonArray.getJSONObject(0).getString("studentid");
			String name = jsonArray.getJSONObject(0).getString("name");
			String major = jsonArray.getJSONObject(0).getString("major");
			String mobile = jsonArray.getJSONObject(0).getString("mobile");
			String message="用户id："+studentid+" 姓名："+name+" 专业："+major+" 电话："+mobile;
			mTextview.append(message);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}


	public String getstudentid(String uid)
	{
		String studentid="";
		try {
			StringBuilder builder = new StringBuilder();
			String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/getstudentid.php";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("uid",uid));
			builder=getResponse(url,params);
			JSONArray jsonArray = new JSONArray(builder.toString());
			studentid= jsonArray.getJSONObject(0).getString("studentid");
		} catch (Exception e) {
			e.printStackTrace();

		}
		return studentid;
	}

	public String getendtime(String aid)
	{
		String endtime="";
		try {
			StringBuilder builder = new StringBuilder();
			String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/getendtime.php";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("aid",aid));
			builder=getResponse(url,params);
			endtime=  builder.toString();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return endtime;
	}
	public String gettitle(String aid)
	{
		String title="";
		try {
			StringBuilder builder = new StringBuilder();
			String url="http://ccnu.chunkao.cn/Devbranch/zcl/signin/gettitle.php";
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("aid",aid));
			builder=getResponse(url,params);
			title=  builder.toString();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return title;
	}
	//访问服务器函数
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressLint("NewApi")
	public StringBuilder getResponse(String url,List<NameValuePair> params)
	{
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		try {
			HttpClient client = new DefaultHttpClient();
			StringBuilder builder = new StringBuilder();
			HttpPost post = new HttpPost(url);

			try {
				post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			try {
				HttpResponse response = client.execute(post);
				HttpEntity entity = response.getEntity();
				BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
				for (String s1 = reader.readLine(); s1 != null; s1 = reader.readLine()) {
					builder.append(s1);
				}

				return builder;
			} catch (Exception e) {
				e.printStackTrace();
				StringBuilder err=new StringBuilder();
				return err.append("error");
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder err=new StringBuilder();
			return err.append("error");
		}
	}


	public boolean  isNetWorkAvailable()
	{
		ConnectivityManager cwjManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cwjManager.getActiveNetworkInfo();
		if (info != null && info.isAvailable()){
			return true;
		}
		else
		{
			Toast.makeText(MainActivity.this,"请联网",Toast.LENGTH_SHORT).show();
			return false;
		}

	}

}


