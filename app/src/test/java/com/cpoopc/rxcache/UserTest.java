package com.cpoopc.rxcache;

import com.cpoopc.rxcache.model.User;

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
public class UserTest {

    @Test
    public void testUser() {
        User user = new User();
        Assert.assertNotNull(user);
        user.setBlog("blog");
        Assert.assertNotNull(user.getBlog());
        Assert.assertEquals(user.getBlog(),"blog");
    }

}
