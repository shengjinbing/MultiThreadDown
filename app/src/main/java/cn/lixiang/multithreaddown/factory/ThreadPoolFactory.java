package cn.lixiang.multithreaddown.factory;

import cn.lixiang.multithreaddown.base.ThreadPoolProxy;

/**
 * Created by Administrator on 2017/7/4 0004.
 */

public class ThreadPoolFactory {
    static ThreadPoolProxy mNormalPool;
    static ThreadPoolProxy	mDownLoadPool;

    /**得到一个普通的线程池*/
    public static ThreadPoolProxy getNormalPool() {
        if (mNormalPool == null) {
            synchronized (ThreadPoolProxy.class) {
                if (mNormalPool == null) {
                    mNormalPool = new ThreadPoolProxy(5, 5, 3000);
                }
            }
        }
        return mNormalPool;
    }
    /**得到一个下载的线程池*/
    public static ThreadPoolProxy getDownLoadPool() {
        if (mDownLoadPool == null) {
            synchronized (ThreadPoolProxy.class) {
                if (mDownLoadPool == null) {
                    mDownLoadPool = new ThreadPoolProxy(6, 6, 3000);
                }
            }
        }
        return mDownLoadPool;
    }
}
