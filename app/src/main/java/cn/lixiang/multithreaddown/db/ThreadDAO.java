package cn.lixiang.multithreaddown.db;

import java.util.List;

import cn.lixiang.multithreaddown.bean.ThreadInfo;

/**
 * Created by Administrator on 2017/7/4 0004.
 */

public interface ThreadDAO {
    // 插入綫程
    public void insertThread(ThreadInfo info);
    // 刪除綫程
    public void deleteThread(String url);
    // 更新綫程
    public void updateThread(String url, int thread_id, long finished,int progressCount);
    // 查詢綫程
    public List<ThreadInfo> queryThreads(String url);
    // 判斷綫程是否存在
    public boolean isExists(String url, int threadId);
}
