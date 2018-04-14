package com.example.administrator.servicebesttest;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    //下载的状态Integer
    public static final int TYPE_SUCCESS = 0;//下载成功
    public static final int TYPE_FAILED = 1;//下载失败
    public static final int TYPE_PAUSED = 2;//下载暂停
    public static final int TYPE_CANCELED = 3;//下载取消

    private DownloadListener listener;//根据下载的状态进行回调
    private boolean isCanceled = false;//记录下载是否被用户取消 可调用cancelDownload()设置
    private boolean isPaused = false;//记录下载是否被用户暂停 可调用pauseDownload()设置
    private int lastProgress;//记录上一次的下载进度

    public DownloadTask(DownloadListener listener) {
        //构造函数
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        //在子线程中执行具体的下载逻辑

        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try {
            long downloadLength = 0;//记录已下载的文件长度
            String downloadUrl = params[0];//从参数中获取下载的URL地址
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));//下载的文件名
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//指定下载路径
            file = new File(directory + fileName);

            if (file.exists()) {
                downloadLength = file.length();//如果文件存在，记录已下载的文件长度
            }

            long contentLength = getContentLength(downloadUrl);//得到待下载的文件长度

            if (contentLength == 0) {
                return TYPE_FAILED;//如果要下载的文件长度为0，则下载失败
            } else if (contentLength == downloadLength) {
                return TYPE_SUCCESS;//已下载的文件长度和文件总长度相等，则下载成功
            }

            //断点下载，从记录的已下载长度开始
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadLength + "-")//指定从哪个字节开始下载
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();

            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadLength);//跳过已下载的字节

                byte[] b = new byte[1024];
                int total = 0;
                int len;

                while ((len = is.read(b)) != -1) {

                    if (isCanceled) {
                        return TYPE_CANCELED;//如果用户取消下载，直接返回，中断下载
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    } else {

                        total += len;//记录下载的长度
                        savedFile.write(b, 0, len);//写回到本地

                        int process = (int) ((total + downloadLength) * 100 / contentLength);//记录下载的百分比

                        publishProgress(process);

                    }

                }

                response.body().close();
                return TYPE_SUCCESS;
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            try {
                if (is != null) {
                    is.close();
                }

                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();//如果用户点了取消，要删除已经下载的文件
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        return TYPE_FAILED;
    }

    //得到待下载的文件长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }

        return 0;


    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);//回调onProgress()更新当前下载进度
            lastProgress = progress;

        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFaild();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;//暂停下载

    }

    public void cancelDownload() {
        isCanceled = true;//取消下载
    }
}
