package com.Aina.Android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

public class ActivityMain extends Activity {

    private static final String TAG = ActivityMain.class.getSimpleName();

    public abstract class HashMapAdapter<K, V> extends BaseAdapter {

        private HashMap<K, V> mData = new HashMap<K, V>();
        private ArrayList<K> mKeys;

        public HashMapAdapter(HashMap<K, V> data) {
            mData = data;
            mKeys = new ArrayList<K>(mData.keySet());
            //Collections.sort(mKeys, Collections.reverseOrder());
        }
        public HashMapAdapter(List<String> lfs)
        {
            for(String s : lfs)
            {
                File f = new File(s);
                mData.put((K)f.getAbsolutePath(), (V)f.getName());
//                mKeys.add((K)f.getAbsolutePath());
            }
            mKeys = new ArrayList<K>(mData.keySet());
            //Collections.sort(mKeys, Collections.reverseOrder());
        }

        public K getKey(int position)
        {
            return (K) mKeys.get(position);
        }
        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public V getItem(int position) {
            return mData.get(mKeys.get(position));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            K key = getKey(pos);
            V value = getItem(pos);

            return onGetView(pos, key, value, convertView, parent );
        }

        protected abstract View onGetView(int pos, K key, V value, View convertView,
                                          ViewGroup parent);
    }



    /**
     * Called when the activity is first created.
     */
    private ViewlrcKLOK viewlrcKLOK;
    private MediaPlayer mMediaPlayer = null;
    private SeekBar mSeekBar = null;
    private ListView mListView = null;
    private ImageButton mLastbtn = null;
    private ImageButton mStartbtn = null;
    private ImageButton mPausebtn = null;
    private ImageButton mStopbtn = null;
    private ImageButton mNextbtn = null;
    private TextView mTextView = null;
    private ScrollView scrollview = null;
    //private ImageView  header =null;
    private TextView lrc = null;
    private List<String> mMusiclist = null;
    private HashMapAdapter<String, String> adapter = null;
    private int mCurrent = 0;
    private int count = 0;
    private boolean isrun = false;
    private boolean isauto = false;
    private static  String PATH = "/sdcard/红宝书/";
    private static TreeMap<Integer, lrcObject> lrc_map;
    private static int lrc_timeend = 0;
    private long KeyDownOldTime = 0;
    private int KeyDownCount = 0;

    private String songName = "";
    private String songAuthor = "";
    private NotificationManager mNotificationManager;
    private static int NOTIFICATIONS_ID = R.layout.main;

    private double playspeed =1.0;
    private boolean bLoopPlay = false;

    public static final int RESULT_CANCELED = -1;
    public static final int RESULT_OK = 0;
    public static final int REQ_SETTINGS = 1;
    public static final int REQUEST_DIRECTORY = 0;

    private void getOverflowMenu() {
        ViewConfiguration viewConfig = ViewConfiguration.get(this);

        try {
            Field overflowMenuField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(null != overflowMenuField){
                overflowMenuField.setAccessible(true);
                overflowMenuField.setBoolean(viewConfig, false);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(ActivityMain.this,Settings.class), REQ_SETTINGS);
                return true;
            case R.id.folder:
                setChooserDirectory();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettingInfo() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final String[] values = getResources().getStringArray(R.array.list_preference);
        String speed = settings.getString(Consts.LIST_KEY, values[0]);
        for(int i = 0; i < values.length; i++)
        {
            if(speed.equalsIgnoreCase(values[i]))
            {
                if(i==0) playspeed = 1.0;
                if(i==1) playspeed = 1.5;
                if(i==2) playspeed = 2.0;
            }
        }
        bLoopPlay = settings.getBoolean(Consts.CHECKOUT_KEY, false);

        //Context context = getActivity();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        PATH = sharedPref.getString(Consts.MEDIA_PATH,"");
        Log.i(TAG, PATH);
    }

    void setChooserDirectory() {
        final Intent chooserIntent = new Intent(
                ActivityMain.this,
                DirectoryChooserActivity.class);

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("DirChooserSample")
                .allowReadOnlyDirectory(true)
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(
                DirectoryChooserActivity.EXTRA_CONFIG,
                config);

        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
    }

    /**
     * 为了得到传回的数据，必须在前面的Activity中（指MainActivity类）重写onActivityResult方法
     *
     * requestCode 请求码，即调用startActivityForResult()传递过去的值
     * resultCode 结果码，结果码用于标识返回数据来自哪个新Activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_SETTINGS && resultCode == RESULT_OK)
        {
            showSettingInfo();
        }else if (requestCode == REQUEST_DIRECTORY) {
            Log.i(TAG, String.format("Return from DirChooser with result %d",
                    resultCode));

            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                PATH = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                Log.i(TAG, PATH);
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(Consts.MEDIA_PATH, PATH);
                editor.commit();
                MusicList();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        getOverflowMenu();
        showSettingInfo();


        mListView = (ListView) this.findViewById(R.id.lv_music);
        mSeekBar = (SeekBar) this.findViewById(R.id.SeekBar01);
        mTextView = (TextView) this.findViewById(R.id.mTextView);
        viewlrcKLOK = (ViewlrcKLOK) this.findViewById(R.id.lrc_view);
        lrc = (TextView) this.findViewById(R.id.lrc);
        scrollview = (ScrollView) this.findViewById(R.id.scrollview);
        mLastbtn = (ImageButton) this.findViewById(R.id.imgbtn_last);
        mStartbtn = (ImageButton) this.findViewById(R.id.imgbtn_start);
        mPausebtn = (ImageButton) this.findViewById(R.id.imgbtn_pause);
        mStopbtn = (ImageButton) this.findViewById(R.id.imgbtn_stop);
        mNextbtn = (ImageButton) this.findViewById(R.id.imgbtn_next);
        //header   = (ImageView) this.findViewById(R.id.header);

        mMusiclist = new ArrayList<String>();
        mMediaPlayer = new MediaPlayer();

        lrc_map = new TreeMap<Integer, lrcObject>();
        viewlrcKLOK.SetlrcContent("歌词", 0, 0, "......", 0);
        lrc_timeend = 0;

        lrc.setVisibility(View.GONE);


        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mMediaPlayer.setOnPreparedListener(prepareListener);
        mMediaPlayer.setOnCompletionListener(CompletionListener);
        mMediaPlayer.setOnErrorListener(ErrorListener);



        // 开始按钮
        mStartbtn.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                //lrc.setVisibility(View.GONE); //控件隐藏
                //PlayMusic(mMusiclist.get(mCurrent));
                PlayMusic(adapter.getKey(mCurrent));
            }

        });
        // 下一首
        mNextbtn.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                NextMusic();
            }

        });
        // 上一首
        mLastbtn.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                LastMusic();
            }

        });
        // 暂停
        mPausebtn.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                isrun = true;
                isauto = false;
                mPausebtn.setVisibility(View.GONE);// 隐藏暂停按钮
                mStartbtn.setVisibility(View.VISIBLE);// 显示启动按钮
                // 是否正在播放
                if (mMediaPlayer.isPlaying()) {
                    viewlrcKLOK.Stop();
                    mMediaPlayer.pause();
                }
            }

        });
        // 停止
        mStopbtn.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                StopMusic();
            }

        });
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                mCurrent = arg2;
                StopMusic();
                //PlayMusic(mMusiclist.get(mCurrent));
                PlayMusic(adapter.getKey(mCurrent));
            }

        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar,
                                          int progress, boolean fromUser) {
                if (!isauto) {
                    mMediaPlayer.seekTo(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isauto = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isauto = true;
            }

        });
        this.MusicList();
    }

    private void setPlayint(String tickerText, String title, String content,
                            int drawable) {
        Notification notification;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ActivityMain.class), 0);
            notification = new Notification.Builder(this.getApplicationContext())
                    .setContentTitle(title)
                    .setContentText(content)
                    .setTicker(tickerText)
                    .setSmallIcon(drawable)
                    .setContentIntent(contentIntent)
                    .build();
        mNotificationManager.notify(NOTIFICATIONS_ID, notification);
    }

    private MediaPlayer.OnPreparedListener prepareListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            isrun = true;
            setPlayint(songAuthor, songName, songAuthor, R.drawable.icon);

            lrc_timeend = 0;
            viewlrcKLOK.SetlrcContent("ready", 0, 0, "go,go,go..", 0);
            viewlrcKLOK.Star();

            count = mMediaPlayer.getDuration();
            Log.i("TAG-count", count + "");
            mSeekBar.setMax(count);// 设置最大值.
            mTextView.setText("当前播放歌曲:" + adapter.getItem(mCurrent));

            mp.start(); //开始播放
            //Toast.makeText(ActivityMain.this, "开始播放",Toast.LENGTH_SHORT).show();
            myHandler.sendEmptyMessage(PROGRESS_CHANGED);
        }
    };

    //视频播放完成
    private MediaPlayer.OnCompletionListener CompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer arg0) {
            mMediaPlayer.stop();
            isauto = true;
            mSeekBar.setProgress(0);

            viewlrcKLOK.SetlrcContent("准备好", 0, 0, "... ...", 0);
            viewlrcKLOK.Stop();
            //Toast.makeText(ActivityMain.this, "播放完成:",Toast.LENGTH_SHORT).show();
            NextMusic();
        }
    };

    //播放时发现错误
    private MediaPlayer.OnErrorListener ErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mMediaPlayer.stop();
            Toast.makeText(ActivityMain.this, "发现错误:-错误编号:" + what, Toast.LENGTH_SHORT).show();
            return true;
        }
    };

    public void PlayMusic(String path) {
        try {

            String lrcpath = path;
            //装载歌词
            lrcpath = lrcpath.substring(0, lrcpath.length() - 4) + ".lrc";
            this.lrc.setText(read(lrcpath));
            //Toast.makeText(this,"打开",Toast.LENGTH_SHORT).show();
            //装载头像

            String headerFile = path.substring(PATH.length());
            int pos1 = headerFile.indexOf("-");
            int pos2 = headerFile.indexOf(".");
            if (pos1 >= 0) {
                songName = headerFile.substring(pos1 + 1, pos2);
                headerFile = headerFile.substring(0, pos1);
                songAuthor = headerFile.trim();

            }else{
                if(pos2>0){
                    songName = headerFile.substring(0, pos2);
                }
            }

//			headerFile = "pic/"+headerFile.trim()+".jpg";

            //Toast.makeText(this, headerFile, Toast.LENGTH_SHORT).show();

//			File myFile = new File(headerFile);
//			if (myFile.exists())
//			{
//			     Bitmap bm = BitmapFactory.decodeFile(headerFile);
//			     header.setImageBitmap(bm);
//			}
//			else
//			{
//				  Bitmap bm = BitmapFactory.decodeResource(this.getResources(),R.drawable.back4);
//				  header.setImageBitmap(bm);
//			}


            mStartbtn.setVisibility(View.GONE);// 隐藏启动按钮
            mPausebtn.setVisibility(View.VISIBLE);// 显示暂停按钮
            if (!isrun) {
                mMediaPlayer.reset();// 重置
                mMediaPlayer.setDataSource(path);// 设置数据源
                mMediaPlayer.prepare();// 准备
            } else {
                mMediaPlayer.start();// 暂停之后接着播放
                viewlrcKLOK.Star();
            }
            isauto = true;
        } catch (Exception ex) {
            this.ShowDialog("播放音乐异常:" + ex.getMessage());
        }
    }


    //创建一个消息处理
    private final static int PROGRESS_CHANGED = 0;
    private final static int HIDE_CONTROLER = 1;
    private final static int VIDEO_READY = 3;
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_READY:
                    break;
                case PROGRESS_CHANGED: {
                    int nowTime = mMediaPlayer.getCurrentPosition();
                    if (nowTime < count) {
                        mSeekBar.setProgress(nowTime);
                        mSeekBar.invalidate();

                        if (nowTime > lrc_timeend && isrun) {
                            //查找歌词
                            Iterator<Integer> iterator = lrc_map.keySet().iterator();
                            while (iterator.hasNext()) {
                                Object ob = iterator.next();
                                lrcObject val = (lrcObject) lrc_map.get(ob);
                                if (nowTime > val.begintime) {
                                    lrcObject val_1 = new lrcObject();
                                    val_1.begintime = 0;
                                    val_1.lrc = "...";
                                    val_1.timeline = 0;

                                    if (iterator.hasNext()) {
                                        Object ob_1 = iterator.next();
                                        val_1 = (lrcObject) lrc_map.get(ob_1);
                                    }

                                    lrc_timeend = val_1.begintime + val_1.timeline;
                                    viewlrcKLOK.SetlrcContent(val.lrc, val.timeline, val_1.begintime - val.begintime - val.timeline, val_1.lrc, val_1.timeline);
                                    viewlrcKLOK.invalidate();
                                }
                            }
                        }

                        nowTime /= 1000;
                        int minute = nowTime / 60;
                        int hour = minute / 60;
                        int second = nowTime % 60;
                        minute %= 60;
                        //mTextView.setText(String.format("%02d",nowTime));

                        mTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                    }
                    sendEmptyMessage(PROGRESS_CHANGED);
                }
                break;
                case HIDE_CONTROLER:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void NextMusic() {
        if(!bLoopPlay) {
            int num = mMusiclist.size();
            if (++mCurrent >= num) {
                mCurrent = 0;
            }
        }
        StopMusic();
        //PlayMusic(mMusiclist.get(mCurrent));
        PlayMusic(adapter.getKey(mCurrent));
    }

    public void LastMusic() {
        if(!bLoopPlay) {
            int num = mMusiclist.size();
            if (--mCurrent < 0) {
                mCurrent = num - 1;
            }
        }
        StopMusic();
        //PlayMusic(mMusiclist.get(mCurrent));
        PlayMusic(adapter.getKey(mCurrent));
    }


    @Override
    protected void onPause() {
        super.onPause();
        setPlayint(songAuthor, songName, songAuthor, R.drawable.icon);
    }


    /**
     * 销毁
     */

    @Override
    protected void onDestroy() {

        isauto = false;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();// 停止
        }
        mMediaPlayer.reset();
        mMediaPlayer.release();

        super.onDestroy();
    }


    protected void dialog() {
        AlertDialog.Builder builder = new Builder(ActivityMain.this);
        builder.setMessage("确定要退出吗?");
        builder.setTitle("提示");
        builder.setPositiveButton("确认",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mNotificationManager.cancel(NOTIFICATIONS_ID);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
        builder.setNegativeButton("取消",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        onPause();
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 79)//耳机键
        {
            long time = System.currentTimeMillis();
            if ((time - KeyDownOldTime) / 1000 > 5)
                KeyDownCount = 0;
            KeyDownCount++;
            Toast.makeText(this, "按键次数:" + event.getRepeatCount() + "所用时间:" + (time - KeyDownOldTime) / 1000, Toast.LENGTH_SHORT).show();
            KeyDownOldTime = time;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            dialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    void startActivitySafely(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
        } catch (SecurityException e) {
        }
    }

    public void StopMusic() {
        isrun = false;

        viewlrcKLOK.Stop();
        lrc_timeend = 99999;
        mPausebtn.setVisibility(View.GONE);// 隐藏暂停按钮
        mStartbtn.setVisibility(View.VISIBLE);// 显示启动按钮

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();// 停止
        }
    }

    /**
     * 文件过滤器
     *
     * @author Aina
     */
    class MusicFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String filename) {

            return (filename.endsWith(".mp3"));
        }

    }

    /**
     * 读取文件
     */
    public static String read(String file) {
        TreeMap<Integer, lrcObject> lrc_read = new TreeMap<Integer, lrcObject>();
        String data = "";
        try {
            File saveFile = new File(file);
            FileInputStream stream = new FileInputStream(saveFile);//  context.openFileInput(file);

            BufferedReader br = new BufferedReader(new InputStreamReader(stream, "GB2312"));
            int i = 0;
            while ((data = br.readLine()) != null) {
                data = data.replace("[", "");
                data = data.replace("]", "@");

                String splitdata[] = data.split("@");
                String lrcContenet = splitdata[splitdata.length - 1];
                for (int j = 0; j < splitdata.length - 1; j++) {
                    String tmpstr = splitdata[j];

                    tmpstr = tmpstr.replace(":", ".");
                    tmpstr = tmpstr.replace(".", "@");
                    String timedata[] = tmpstr.split("@");

                    int m = Integer.parseInt(timedata[0]);  //分
                    int s = Integer.parseInt(timedata[1]);  //秒
                    int ms = Integer.parseInt(timedata[2]); //毫秒
                    int currTime = (m * 60 + s) * 1000 + ms * 10;
                    lrcObject item1 = new lrcObject();

                    item1.begintime = currTime;
                    item1.lrc = lrcContenet;
                    lrc_read.put(currTime, item1);// 图像资源的ID
                    i++;
                }

            }
            stream.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
		
		/*
		 * 遍历hashmap 计算每句歌词所需要的时间
		*/
        lrc_map.clear();
        data = "";
        Iterator<Integer> iterator = lrc_read.keySet().iterator();
        lrcObject oldval = null;
        int i = 0;
        StringBuffer sb = new StringBuffer();
        while (iterator.hasNext()) {
            Object ob = iterator.next();

            lrcObject val = (lrcObject) lrc_read.get(ob);
            if (oldval == null)
                oldval = val;
            else {
                lrcObject item1 = new lrcObject();
                item1 = oldval;
                item1.timeline = val.begintime - oldval.begintime;
                lrc_map.put(new Integer(i), item1);
                sb.append(String.format("[%04d]-[%04d]-%s\n", item1.begintime, item1.timeline, item1.lrc));
                i++;
                oldval = val;
            }
        }
        data = sb.toString();
        return data;
    }

    /**
     * 播放列表
     */
    private void AddMusicList(String spath) {
        try {
            File subdir = new File(spath);
            File[] fs = subdir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return dir.isDirectory();
                }
            });
            for (File f : fs) {
                if (f.isDirectory()) {
                    AddMusicList(f.getPath());
                }
            }
            File home = new File(spath);
            File[] f = home.listFiles(new MusicFilter());
            if (f.length > 0) {
                for (int i = 0; i < f.length; i++) {
                    File file = f[i];
                    mMusiclist.add(file.getAbsolutePath());//file.getName().toString());
                }
            }
        } catch (Exception ex) {

        }

    }

    public void MusicList() {
        try {
//			File home = new File(PATH);
//			File[] f = home.listFiles(new MusicFilter());
//			if (f.length > 0) {
//				for (int i = 0; i < f.length; i++) {
//					File file = f[i];
//					mMusiclist.add(file.getName().toString());
//				}
//				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//						android.R.layout.simple_list_item_1, mMusiclist);
//				mListView.setAdapter(adapter);
//			}
            mMusiclist.clear();
            AddMusicList(PATH);
            if (mMusiclist.size() > 0) {
                //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMusiclist);
                adapter = new HashMapAdapter<String, String>(mMusiclist) {
                    @Override
                    protected View onGetView(int pos, String key, String value, View convertView, ViewGroup parent) {
                        final View result;

                        if (convertView == null) {
                            result = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                        } else {
                            result = convertView;
                        }
                        ((TextView) result.findViewById(android.R.id.text1)).setText(value);
                        return result;
                    }
                };

                mListView.setAdapter(adapter);
            } else {
                setChooserDirectory();
            }
        } catch (Exception ex) {
            this.ShowDialog("显示音乐列表异常:" + ex.getMessage());
        }

    }

    public void ShowDialog(String str) {
        new AlertDialog.Builder(this).setTitle("提示").setMessage(str)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }

                }).show();
    }

}