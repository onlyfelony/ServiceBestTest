package com.example.administrator.servicebesttest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private DownloadBinder mBinder = new DownloadBinder();
    private DownloadTask downloadTask;
    private String urlDownload;

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("Downloading....",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask = null;

            //取消前台通知并且创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download success",-1));

            Toast.makeText(DownloadService.this,"Download success",Toast.LENGTH_SHORT).show();


        }

        @Override
        public void onFaild() {
            downloadTask = null;

            //取消前台通知并且创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Filed",-1));

            Toast.makeText(DownloadService.this,"Download Filed",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask = null;
            Toast.makeText(DownloadService.this,"paused",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this,"canceled",Toast.LENGTH_SHORT).show();
        }
    };

    class DownloadBinder extends Binder {

        //开始下载
        public void startDownload(String url){
            if(downloadTask==null) {
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(url);
                urlDownload = url;

                //将下载通知转到前台
                startForeground(1,getNotification("Downloading...",0));
                Toast.makeText(DownloadService.this,"Downloading..",Toast.LENGTH_SHORT).show();

            }

        }

        //暂停下载
        public void pauseDownload(){
            if (downloadTask!=null){
                downloadTask.pauseDownload();
            }

        }

        //取消下载
        public void cancelDownload(){
            if (downloadTask!=null){
                downloadTask.cancelDownload();
            }

            if(urlDownload!=null){
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                String fileName = urlDownload.substring(urlDownload.lastIndexOf("/"));
                File file = new File(directory+fileName);

                if (file.exists()){
                    file.delete();//如果文件存在则删除
                }
                getNotificationManager().cancel(1);//取消通知
                stopForeground(true);
                Toast.makeText(DownloadService.this,"Canceled..",Toast.LENGTH_SHORT).show();
            }


        }


    }


    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title ,int progress){

        String channalId = null;

        if(Build.VERSION.SDK_INT>=26){
            channalId = "mChannelId";
            NotificationChannel channel = new NotificationChannel(channalId,"MyChannel",NotificationManager.IMPORTANCE_DEFAULT);
            getNotificationManager().createNotificationChannel(channel);
        }


        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pi =  PendingIntent.getActivity(this,0,intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,channalId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if(progress>=0){
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);//进度条
        }

        return builder.build();

    }

}
