package io.jween.onlydownload.sample;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

import io.jween.onlydownload.R;
import io.jween.onlydownload.downloader.DownloadState;
import io.jween.onlydownload.downloader.FileDownloader;
import io.jween.onlydownload.downloader.XmlConfigs;
import io.jween.onlydownload.file.DiskUtil;
import io.jween.onlydownload.model.DownloadProgress;
import io.jween.onlydownload.namer.HashFileNamer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class SampleActivity extends AppCompatActivity {

    TextView console;
    CompositeDisposable cd = new CompositeDisposable();

    // small image file
    static final String IMG_URL = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545822659778&di=04b9f477fa6b502004d444154365f6b4&imgtype=0&src=http%3A%2F%2Fatt.bbs.duowan.com%2Fforum%2F201309%2F27%2F121732olrimy3dkxgrmdyl.jpg";
    // huge apk file
    static final String APK_URL = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h8218_1.43.1.15_fc9dc4.apk";

    String downloadUrl;

    FileDownloader fd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });



        console = findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());

        console.append("External free space is " + DiskUtil.freeSpace(true) + "MB" + "\n");
        console.append("Download SubDir is " + XmlConfigs.from(this).downloadSubFolder + "\n");


        downloadUrl = APK_URL;
        fd = new FileDownloader(this, downloadUrl, new HashFileNamer());
        showProgress(downloadUrl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cd.clear();
    }

    public void onStartAndPause(View v) {
        final Button btn = (Button) v;


        DownloadState state = fd.getLatestDownloadState();
        if (state == DownloadState.PAUSED ||
                state == DownloadState.PENDING) {
            console.append("点击开始\n");
            fd.start();
            btn.setText("暂停");
        } else if (state == DownloadState.DOWNLOADING) {
            console.append("点击暂停\n");
            fd.pause();
            btn.setText("开始");
        }
    }

    private void showProgress(String url) {
        Disposable d = fd.listenToProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DownloadProgress>() {
                    @Override
                    public void accept(DownloadProgress progress) throws Exception {
                        String msg = "速度 "
                                + (1.f * progress.downloadSpeed / 1024) + "KB/s, "
                                + (1.f * progress.downloadedSize / (1024 * 1024)) + "MB/"
                                + (1.f * progress.totalSize / (1024 * 1024)) + "MB, "
                                + progress.downloadState.name();
                        console.append(msg + "\n");
                    }
                });
        cd.add(d);
    }

    private void rm() {
        File workspace = new File(DiskUtil.getDownloadDir(), XmlConfigs.from(this).downloadSubFolder);
        delete(workspace);
    }

    private void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }

        if (f.exists()) {
            f.delete();
        }
    }
}
