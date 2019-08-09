package com.marvin.iftyvoicetest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean mTranslateEnable = false;
    private KqwSpeechSynthesizer mKqwSpeechSynthesizer;
    protected String[] needPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private boolean isNeedCheck = true;
    private static final int PERMISSON_REQUESTCODE = 0;
    private SpeechRecognizer mIat;
    private String mTrans;
    private String mOris;
    private String mText;
    private TextView mTv_test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        mTv_test = findViewById(R.id.tv_test);
        // 初始化语音合成对象
//

    }

    // 获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        // 合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet"));
        tempBuffer.append(";");
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/sms_16k.jet"));
        //识别8k资源-使用8k的时候请解开注释
        return tempBuffer.toString();
    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("clz", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败，错误码：" + code);
                Toast.makeText(MainActivity.this, "初始化失败，错误码" + code, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Toast.makeText(MainActivity.this, "开始说话", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if (mTranslateEnable && error.getErrorCode() == 14002) {
//                showTip(error.getPlainDescription(true) + "\n请确认是否已开通翻译功能");
//                Toast.makeText(MainActivity.this, "错误", Toast.LENGTH_SHORT).show();
            } else {
//                showTip(error.getPlainDescription(true));
//                Toast.makeText(MainActivity.this, error.getPlainDescription(true), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");
            Toast.makeText(MainActivity.this, "结束说话", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            if (mTranslateEnable) {
//                printTransResult(results);
            } else {
                mText = JsonParser.parseIatResult(results.getResultString());
//                Toast.makeText(MainActivity.this, ""+ mText, Toast.LENGTH_SHORT).show();
            }

            if (isLast) {
                //TODO 最后的结果
                String last = mTrans + mOris + mText;
                mTv_test.setText(last);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小：" + volume);
            Toast.makeText(MainActivity.this, "当前说话音量太小", Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //    String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //    Log.d(TAG, "session id =" + sid);
            // }
        }
    };


    /**
     * 开始合成
     *
     * @param view
     */
    public void start(View view) {
        //1.创建RecognizerDialog对象
        RecognizerDialog recognizerDialog = new RecognizerDialog(MainActivity.this, mInitListener);
        //2.设置accent、language等参数
        recognizerDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");//语种，这里可以有zh_cn和en_us
        recognizerDialog.setParameter(SpeechConstant.ACCENT, "mandarin");//设置口音，这里设置的是汉语普通话 具体支持口音请查看讯飞文档，
        recognizerDialog.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");//设置编码类型
        recognizerDialog.setParameter(SpeechConstant.VAD_BOS, "4000");
        //设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
        recognizerDialog.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
        recognizerDialog.setParameter(SpeechConstant.SUBJECT, null);
//设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
        recognizerDialog.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
//自动停止录音，范围{0~10000}
        recognizerDialog.setParameter(SpeechConstant.VAD_EOS, "1000");
//设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        recognizerDialog.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        if (SpeechConstant.TYPE_LOCAL.equals(SpeechConstant.TYPE_LOCAL)) {
            // 设置本地识别资源
            recognizerDialog.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        }

        //其他设置请参考文档http://www.xfyun.cn/doccenter/awd
        //3.设置讯飞识别语音后的回调监听
        recognizerDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {//返回结果
                Log.i("test_xunfei", recognizerResult.getResultString());
//                result(recognizerResult.getResultString());
                mTv_test.setText(result(recognizerResult.getResultString()));
//                StringBuffer stringBuffer = new StringBuffer();
//                String text = result(recognizerResult.getResultString());
//                stringBuffer.append(text);
//                String result = stringBuffer.toString();
//                mTv_test.setText(result);
            }

            @Override
            public void onError(SpeechError speechError) {//返回错误
                Log.e("test_xunfei", speechError.getErrorCode() + "");
            }

        });
        //显示讯飞语音识别视图
        recognizerDialog.show();

//        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
//
////设置语法ID和 SUBJECT 为空，以免因之前有语法调用而设置了此参数；或直接清空所有参数，具体可参考 DEMO 的示例。
//        mIat.setParameter( SpeechConstant.CLOUD_GRAMMAR, null );
//        mIat.setParameter( SpeechConstant.SUBJECT, null );
////设置返回结果格式，目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容
//        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
////此处engineType为“cloud”
//        mIat.setParameter( SpeechConstant.ENGINE_TYPE, "local");
//        if (SpeechConstant.ENGINE_TYPE.equals(SpeechConstant.TYPE_LOCAL)) {
//            // 设置本地识别资源
//            mIat.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
//        }
////设置语音输入语言，zh_cn为简体中文
//        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
////设置结果返回语言
//        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
//// 设置语音前端点:静音超时时间，单位ms，即用户多长时间不说话则当做超时处理
////取值范围{1000～10000}
//        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
////设置语音后端点:后端点静音检测时间，单位ms，即用户停止说话多长时间内即认为不再输入，
////自动停止录音，范围{0~10000}
//        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
////设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
//        mIat.setParameter(SpeechConstant.ASR_PTT,"1");
//
////开始识别，并设置监听器
//        mIat.startListening(mRecognizerListener);
    }

    public String result(String resultString) {
        JSONObject jsonObject = JSON.parseObject(resultString);
        JSONArray jsonArray = jsonObject.getJSONArray("ws");
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
            JSONArray jsonArray1 = jsonObject1.getJSONArray("cw");
            JSONObject jsonObject2 = jsonArray1.getJSONObject(0);
            String w = jsonObject2.getString("w");
            stringBuffer.append(w);
        }
        String result = stringBuffer.toString();
        Log.i("test_xunfei", "识别结果为：" + result);
        return result;
    }

    private void initPermission() {
        if (isNeedCheck) {
            checkPermissions(needPermissions);
        }
    }

    private void checkPermissions(String... permissions) {
        //获取权限列表
        List<String> needRequestPermissonList = findDeniedPermissions(permissions);
        if (null != needRequestPermissonList
                && needRequestPermissonList.size() > 0) {
            //list.toarray将集合转化为数组
            ActivityCompat.requestPermissions(this,
                    needRequestPermissonList.toArray(new String[needRequestPermissonList.size()]),
                    PERMISSON_REQUESTCODE);
        }
    }

    private List<String> findDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissonList = new ArrayList<String>();
        //for (循环变量类型 循环变量名称 : 要被遍历的对象)
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    perm) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, perm)) {
                needRequestPermissonList.add(perm);
            }
        }
        return needRequestPermissonList;
    }

    private boolean verifyPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        if (requestCode == PERMISSON_REQUESTCODE) {
            if (verifyPermissions(paramArrayOfInt)) {
                isNeedCheck = false;
//                SPUtil.putBoolean(this,"isNeedCheck",false);
            } else {
                Toast.makeText(this, "你拒绝了该权限信息", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
///MscRecognizer: rsltCb:0result:{"sn":1,"ls":false,"bg":0,"ed":0,"ws":[{"bg":0,"cw":[{"sc":0.0,"w":"没有"}]}]}
//MscRecognizer: rsltCb:5result:{
//      "sn":1,
//      "ls":true,
//      "bg":0,
//      "ed":0,
//      "ws":[{
//          "bg":0,
//          "slot":"WFST",
//          "cw":[{
//              "w":"陪你有。",
//              "sc":0
//            }]
//        }],
//      "sc":0
//    }