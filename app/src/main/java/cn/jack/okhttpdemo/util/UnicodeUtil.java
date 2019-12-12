package cn.jack.okhttpdemo.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cyg on 2019-07-30.
 */
public class UnicodeUtil {

    /*
     * unicode编码转中文
     */
    public static String decodeUnicode(String dataStr) {
        if (TextUtils.isEmpty(dataStr)) {
            return "";
        }
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(dataStr);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            dataStr = dataStr.replace(matcher.group(1), ch + "");
        }
        return dataStr;
    }
}
