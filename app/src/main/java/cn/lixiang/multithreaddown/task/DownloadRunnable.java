package cn.lixiang.multithreaddown.task;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import cn.lixiang.multithreaddown.bean.FileInfo;

import static cn.lixiang.multithreaddown.service.DownloadService.ACTION_START;

/**
 * Created by Administrator on 2017/7/4 0004.
 */

public class DownloadRunnable implements Runnable {
    public static final int MSG_INIT = 0;
    private Context mContext;

    private FileInfo mFileInfo = null;
    // 文件的保存路徑
    public static final String DownloadPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/download/";
    private Map<Integer, DownloadTask> mTasks;

    public DownloadRunnable(FileInfo fileInfo, Context context, Map<Integer, DownloadTask> tasks) {
        mFileInfo = fileInfo;
        mContext = context;
        mTasks = tasks;
    }


    // 從InitThread綫程中獲取FileInfo信息，然後開始下載任務
    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_INIT:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    Log.i("test", "INIT:" + fileInfo.toString());
                    // 獲取FileInfo對象，開始下載任務
                    DownloadTask task = new DownloadTask(mContext, fileInfo, 3);
                    task.download();
                    // 把下载任务添加到集合中
                    mTasks.put(fileInfo.getId(), task);
                    // 发送启动下载的通知
                    Intent intent = new Intent(ACTION_START);
                    intent.putExtra("fileInfo", fileInfo);
                    mContext.sendBroadcast(intent);
                    break;
            }
        }

        ;
    };

    @Override
    public void run() {
        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        try {
            URL url = new URL(mFileInfo.getUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            int length = -1;
            if (code == HttpURLConnection.HTTP_OK) {
                length = conn.getContentLength();
                Log.d("BBBBB","wenjiangleng"+length);
            }
            //如果文件长度为小于0，表示获取文件失败，直接返回
            if (length <= 0) {
                return;
            }
            // 判斷文件路徑是否存在，不存在這創建
            File dir = new File(DownloadPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            // 創建本地文件
            File file = new File(dir, mFileInfo.getFileName());
            raf = new RandomAccessFile(file, "rwd");
            raf.setLength(length);
            // 設置文件長度
            mFileInfo.setLength(length);
            // 將FileInfo對象傳遞給Handler
            Message msg = Message.obtain();
            msg.obj = mFileInfo;
            msg.what = MSG_INIT;
            mHandler.sendMessage(msg);
            //msg.setTarget(mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
