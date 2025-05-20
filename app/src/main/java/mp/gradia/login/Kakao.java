package mp.gradia.login;

import android.app.Application;

import com.kakao.sdk.common.KakaoSdk;

public class Kakao extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 네이티브 앱 키로 Kakao SDK 초기화
        KakaoSdk.init(this, "0ae3bc452c369df08a11b5fd073d3df9");
    }
}

/* 카카오 SDK 불러오는 Activity */
