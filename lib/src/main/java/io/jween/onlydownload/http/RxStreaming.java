package io.jween.onlydownload.http;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxStreaming {
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB
    private static final OkHttpClient CLIENT = createOkHttpClient();


    private static OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(15 * 1000, TimeUnit.SECONDS)
                .readTimeout(15 * 1000, TimeUnit.MILLISECONDS);
        return builder.build();
    }

    /**
     * 从 url 的文件中读取从 rangeStart 开始的 length 长度
     * 举例:
     *    表示头500个字节：Range: bytes=0-499
     *    表示第二个500字节：Range: bytes=500-999
     *    表示最后500个字节：Range: bytes=-500
     *    表示500字节以后的范围：Range: bytes=500-
     *    第一个和最后一个字节：Range: bytes=0-0,-1
     *    同时指定几个范围：Range: bytes=500-600,601-999
     * @param url url
     * @param rangeStart http 分片下载开始位置, 不小于 0
     * @param length http 分片下载长度, 如果为 0, 则不指定长度, 从 rangeStart 位置开始一直下载
     * @return Request.
     */
    private static Request buildRequest(String url, long rangeStart, long length) {
        Request.Builder builder = new Request.Builder();
        builder.url(url)
                .addHeader("Content-Type", "application/json")
                .cacheControl(new CacheControl.Builder().noCache().noStore().build());

        if (rangeStart >= 0 && length >= 0) {
            String rangeValue = "bytes=" + rangeStart + "-";
            if (length > 0) {
                rangeValue += rangeStart + length - 1;
            }
            builder.header("RANGE", rangeValue);
        }
        return builder.build();
    }

    public static Single<Response> request(String url, long rangeStart, long length) {
        Request request = buildRequest(url, rangeStart, length);
        final Call call = CLIENT.newCall(request);
        return Single.create(new SingleOnSubscribe<Response>() {
            @Override
            public void subscribe(SingleEmitter<Response> emitter) {
                Response response;
                try {
                    response = call.execute();
                    if (emitter.isDisposed()) {
                        return;
                    }
                    if (response.isSuccessful()) {
                        emitter.onSuccess(response);
                    } else if (response.isRedirect() ) {
                        // @see followRedirects(true)
                        // 重定向
                    } else {
                        // 40X, 50X 在这里处理
                        emitter.onError(new Exception("request 未处理的异常"));
                    }
                } catch (IOException e) {
                    if (emitter.isDisposed()) {
                        return;
                    } else {
                        emitter.onError(e);
                    }
                }


            }
        }).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                if (!call.isCanceled() && !call.isExecuted() ) {
                    call.cancel();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    public static Single<Response> request(String url) {
        return request(url, 0, 0);
    }

    /**
     * get download streaming from given rangeStart.
     * @param url
     * @param rangeStart
     * @return
     */
    public static Single<Response> request(String url, long rangeStart) {
        return request(url, rangeStart, 0);
    }

    public static Flowable<byte[]> download(final String url, final long rangeStart, final long length) {
        return request(url, rangeStart, length)
                .flatMapPublisher(new Function<Response, Publisher<byte[]>>() {
                    @Override
                    public Publisher<byte[]> apply(final Response response) {
                        return Flowable.generate(new Callable<InputStream>() {
                            @Override
                            public InputStream call() throws Exception {
                                return response.body().byteStream();
                            }
                        }, new BiConsumer<InputStream, Emitter<byte[]>>() {
                            @Override
                            public void accept(InputStream is, Emitter<byte[]> emitter) throws Exception {
                                int bufferSize = DEFAULT_BUFFER_SIZE;
                                byte[] buffer = new byte[bufferSize];
                                int count = 0;
                                try {
                                    count = is.read(buffer);
                                } catch (IOException e) {
                                    emitter.onError(e);
                                    return;
                                }

                                if (count == -1) {
                                    emitter.onComplete();
                                } else if (count < bufferSize) {
                                    emitter.onNext(Arrays.copyOf(buffer, count));
                                } else {
                                    emitter.onNext(buffer);
                                }
                            }
                        }, new Consumer<InputStream>() {
                            @Override
                            public void accept(InputStream is) throws Exception {
                                is.close();
                            }
                        });
                    }
                });
    }
}
