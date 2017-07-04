package cn.lixiang.multithreaddown.task;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.lixiang.multithreaddown.bean.FileInfo;
import cn.lixiang.multithreaddown.bean.ThreadInfo;
import cn.lixiang.multithreaddown.db.ThreadDAO;
import cn.lixiang.multithreaddown.db.ThreadDAOImple;
import cn.lixiang.multithreaddown.factory.ThreadPoolFactory;
import cn.lixiang.multithreaddown.service.DownloadService;

/**
 * Created by Administrator on 2017/7/4 0004.
 */

public class DownloadTask {
    private Context mComtext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAO mDao = null;
    //这里特别注意了，有些文件胡超出int值的范围
    private long mFinished = 0;
    private int mThreadCount = 1;
    public boolean mIsPause = false;
    private List<MultiDownloadThreadRunnable> mThreadlist = null;
    public static ExecutorService sExecutorService = Executors.newCachedThreadPool();

    public DownloadTask(Context comtext, FileInfo fileInfo, int threadCount) {
        super();
        this.mThreadCount = threadCount;
        this.mComtext = comtext;
        this.mFileInfo = fileInfo;
        this.mDao = new ThreadDAOImple(mComtext);
    }

    public void download() {
        // 从数据库中获取下载的信息
        List<ThreadInfo> list = mDao.queryThreads(mFileInfo.getUrl());
        if (list.size() == 0) {
            long length = mFileInfo.getLength();
            long block = length / mThreadCount;
            for (int i = 0; i < mThreadCount; i++) {
                // 划分每个线程开始下载和结束下载的位置
                long start = i * block;
                long end = (i + 1) * block - 1;
                if (i == mThreadCount - 1) {
                    end = length - 1;
                }
                ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), start, end, 0);
                list.add(threadInfo);
                // 如果數據庫不存在下載信息，添加下載信息
                mDao.insertThread(threadInfo);
            }
        }

        mThreadlist = new ArrayList<MultiDownloadThreadRunnable>();
        for (ThreadInfo info : list) {
            MultiDownloadThreadRunnable thread = new MultiDownloadThreadRunnable(info);
            ThreadPoolFactory.getDownLoadPool().execute(thread);
            mThreadlist.add(thread);
        }
    }

    public synchronized void checkAllFinished() {
        boolean allFinished = true;
        for (MultiDownloadThreadRunnable thread : mThreadlist) {
            if (!thread.isFinished) {
                allFinished = false;
                break;
            }
        }
        if (allFinished == true) {
            Log.i("BBBBB", "全部下载完成");
            // 下載完成后，刪除數據庫信息
            mDao.deleteThread(mFileInfo.getUrl());
            // 通知UI哪个线程完成下载
            Intent intent = new Intent(DownloadService.ACTION_FINISHED);
            intent.putExtra("fileInfo", mFileInfo);
            mComtext.sendBroadcast(intent);

        }
    }

    class MultiDownloadThreadRunnable implements Runnable {
        private ThreadInfo threadInfo = null;
        // 标识线程是否执行完毕
        public boolean isFinished = false;

        public MultiDownloadThreadRunnable(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        public void run() {

            HttpURLConnection conn = null;
            RandomAccessFile raf = null;
            InputStream is = null;
            try {
                URL url = new URL(mFileInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setRequestMethod("GET");

                long start = threadInfo.getStart() + threadInfo.getFinished();
                // 設置下載文件開始到結束的位置
                conn.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.getEnd());
                Log.i("BBBBB", "Start==" + start + "," + "END==" + threadInfo.getEnd());
                File file = new File(DownloadService.DownloadPath, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                mFinished += threadInfo.getFinished();

                Intent intent = new Intent();
                intent.setAction(DownloadService.ACTION_UPDATE);

                int code = conn.getResponseCode();
                //206 这种响应是在客户端表明自己只需要目标URL上的部分资源的时候返回的
                if (code == HttpURLConnection.HTTP_PARTIAL) {
                    is = conn.getInputStream();
                    byte[] bt = new byte[1024];
                    int len = -1;
                    // 定义UI刷新时间
                    long time = System.currentTimeMillis();
                    while ((len = is.read(bt)) != -1) {
                        raf.write(bt, 0, len);
                        // 累计整个文件完成进度
                        mFinished += len;
                        // 累加每个线程完成的进度
                        threadInfo.setFinished(threadInfo.getFinished() + len);
                        //随时保持进度条的进度
                        threadInfo.setProgressCount((int) (mFinished * 100 / mFileInfo.getLength()));
                        //不断的更新数据库
                        mDao.updateThread(threadInfo.getUrl(), threadInfo.getId(), threadInfo.getFinished(), threadInfo.getProgressCount());
                        // 設置爲500毫米更新一次
                        if (System.currentTimeMillis() - time > 1000) {
                            time = System.currentTimeMillis();
                            // 发送已完成多少
                            intent.putExtra("finished", (int)(mFinished * 100 / mFileInfo.getLength()));
                            // 表示正在下载文件的id
                            intent.putExtra("id", mFileInfo.getId());
                            // 发送广播给Activity
                            mComtext.sendBroadcast(intent);

                            Log.d("BBBBB", (int)(mFinished * 100 / mFileInfo.getLength())+"");
                        }
                        if (mIsPause) {
                            Log.d("BBBBB", "暂停");
                            //mDao.updateThread(threadInfo.getUrl(), threadInfo.getId(), threadInfo.getFinished(), threadInfo.getProgressCount());
                            return;
                        }
                    }
                }
                // 标识线程是否执行完毕
                isFinished = true;
                // 判断是否所有线程都执行完毕
                checkAllFinished();

            } catch (Exception e) {
                Log.d("BBBBB", "发生异常");
                e.printStackTrace();
            } finally {
                Log.d("BBBBB", "总是会被执行");
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
