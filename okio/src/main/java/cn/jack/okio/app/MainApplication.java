package cn.jack.okio.app;

import android.app.Application;

/**
 * Created by cyg on 2019-09-09.
 */
public class MainApplication extends Application {

    private static MainApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static MainApplication getInstance() {
        return mInstance;
    }
}
