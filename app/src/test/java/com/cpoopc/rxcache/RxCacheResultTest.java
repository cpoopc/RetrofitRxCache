package com.cpoopc.rxcache;

import com.cpoopc.retrofitrxcache.RxCacheResult;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * @author cpoopc
 * @date 2016/06/29
 * @time 22:53
 * @description
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RxCacheResultTest {

    @Test
    public void testResult() {
        RxCacheResult<String> rxCacheResult = new RxCacheResult(true, "哈哈");
        Assert.assertNotNull(rxCacheResult);
        Assert.assertTrue(rxCacheResult.isCache());
    }

}
