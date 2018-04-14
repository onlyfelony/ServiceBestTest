package com.example.administrator.servicebesttest;

public interface DownloadListener {

    void onProgress(int progress);//通知当前的下载进度
    void onSuccess();//通知下载成功事件
    void onFaild();//通知下载失败事件
    void onPaused();//通知下载暂停事件
    void onCanceled();//通知下载取消事件


}
