package cn.lixiang.multithreaddown.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.lixiang.multithreaddown.bean.FileInfo;
import cn.lixiang.multithreaddown.factory.ThreadPoolFactory;
import cn.lixiang.multithreaddown.task.DownloadRunnable;
import cn.lixiang.multithreaddown.task.DownloadTask;

/**
 * Created by Administrator on 2017/7/4 0004.
 */

public class DownloadService extends Service{

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final String ACTION_FINISHED = "ACTION_FINISHED";
    // 文件的保存路徑
    public static final String DownloadPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/download/";

    private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<Integer, DownloadTask>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 获得Activity穿来的参数
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("BBBBB", "START" + fileInfo.toString());

            ThreadPoolFactory.getNormalPool().execute(new DownloadRunnable(fileInfo,this,mTasks));
        } else if (ACTION_STOP.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("BBBBB", "END" + fileInfo.toString());

            DownloadTask task = mTasks.get(fileInfo.getId());
            Log.i("BBBBB", mTasks.size()+"66666");
            if (task != null) {
                // 停止下载任务
                task.mIsPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
