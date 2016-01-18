package com.cpoopc.retrofitrxcache;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 提供MD5加密方法
 * <p>
 * Description:描述
 * </p>
 */
public class MD5 {

    private static final String TAG = "MD5";

    /**	
     * 对输入文本进行MD5加密
     * @param info 被加密文本，如果输入为null则返回null
     * @return MD5 加密后的内容，如果为null说明输入为null
     */
    public static String getMD5(String info) {
        return getMD5(info.getBytes());
    }

    /**	
     * 对输入文本进行MD5加密
     * @param info 被加密文本 如果输入为null则返回null
     * @return MD5加密后的内容，如果为null说明输入为null
     */
    public static String getMD5(byte[] info) {
        if (null == info || info.equals("")) {
            return null;
        }
        StringBuffer buf = new StringBuffer("");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        md.update(info);
        byte b[] = md.digest();
        int i;

        for (int offset = 0; offset < b.length; offset++) {
            i = b[offset];
            if (i < 0)
                i += 256;
            if (i < 16)
                buf.append("0");
            buf.append(Integer.toHexString(i));
        }
        return buf.toString();
    }

}
