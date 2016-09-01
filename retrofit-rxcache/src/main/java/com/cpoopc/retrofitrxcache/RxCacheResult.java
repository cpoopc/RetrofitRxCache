package com.cpoopc.retrofitrxcache;

/**
 * User: cpoopc
 * Date: 2016/2/28
 * Time: 21:14
 */
public class RxCacheResult<T> {
    private boolean isCache;
    private T resultModel;

    public RxCacheResult(boolean isCache, T resultModel) {
        this.isCache = isCache;
        this.resultModel = resultModel;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    public T getResultModel() {
        return resultModel;
    }

    public void setResultModel(T resultModel) {
        this.resultModel = resultModel;
    }
}
