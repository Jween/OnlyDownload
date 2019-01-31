package io.jween.onlydownload.model;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;

public abstract class RxInMemHolder<T> {
    private T inMem;
    private final Object inMemLock = new Object[0];

    void setInMem(T t) {
        synchronized (inMemLock) {
            this.inMem = t;
        }
    }

    T getInMem() {
        synchronized (inMemLock) {
            return inMem;
        }
    }

    /**
     * 使用者, 异步获取该资源
     * @return
     */
    public Single<T> get() {
        synchronized (inMemLock) {
            if (inMem != null) {
                return Single.just(inMem);
            } else {
                return provide().doAfterSuccess(new Consumer<T>() {
                    @Override
                    public void accept(T t) throws Exception {
                        setInMem(t);
                    }
                });
            }
        }
    }

    protected abstract Single<T> provide();
}
