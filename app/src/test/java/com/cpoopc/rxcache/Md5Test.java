package com.cpoopc.rxcache;

import com.cpoopc.retrofitrxcache.MD5;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
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
public class Md5Test {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testMd5() {
        String content = "待测文本";
        Assert.assertEquals(MD5.getMD5(content),MD5.getMD5(content));
        Assert.assertNotSame(MD5.getMD5(content), MD5.getMD5(content + '1'));
    }
}
