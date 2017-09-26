package com.example.zpushtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zpushtest.base.Key;
import com.example.zpushtest.bean.Function;
import com.example.zpushtest.dialog.MaterialDialog;
import com.example.zpushtest.utils.Engine;
import com.example.zpushtest.utils.SP;
import com.zzwtec.distributedpush.api.ZZWPush;
import com.zzwtec.distributedpush.push.IPushClient;
import com.zzwtec.distributedpush.push.PushResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView mTvAlias;
    private TextView mTvHeartbeat;
    private ListView mLvTopics;
    private EditText mEtAlias;
    private EditText mEtHeartbeat;
    private EditText mEtTopics;
    private MaterialDialog mMaterialDialog;
    private View root;
    private ArrayList<String> objects;
    private ArrayAdapter<String> adapter;
    private LinearLayout mLlTopic;
    private HorizontalScrollView mHSV;

    private Map<String, Function> functions = new HashMap<>();
    private MaterialDialog materialDialog;
    private TextView mTvRequest;
    private TextView mTvResponse;
    private LinearLayout mLlContainer;

    private String push_to_alias = "{\"t\":6," +
            "\"method\":\"publish_to_alias\"," +
            "\"alias\":\"%s\"," +
            "\"msg\":\"%s\"," +
            "\"opts\":{" +
            "\"time_to_live\":%d" +
            "}," +
            "\"num\":%d" + "}";

    private String push_to_topics = "{\"t\":6," +
            "\"method\":\"publish\"," +
            "\"topic\":[%s]," +
            "\"msg\":\"%s\"," +
            "\"opts\":{" +
            "\"time_to_live\":%d" +
            "}," +
            "\"num\":%d" + "}";
    private MaterialDialog mInputsDialog;

    private String action;
    private List<Object> params = new ArrayList<>();
    private TextView mTvPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        showDialog("请先设置推送参数");

        registerReceiver(mPushReceiver, new IntentFilter(ZZWPush.ACTION.PUSH_ARRIVE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPushReceiver);
    }

    private void fillFunctions() {
        functions.put("HeartBeat",
                new Function(
                        "",
                        "HeartBeat",
                        "{\"t\":1,\"alias\":\"%s\"}", new Runnable() {
                    @Override
                    public void run() {
                        Function heartBeat = functions.get("HeartBeat");
                        String requestJson = String.format(heartBeat.requestJson, SP.get(Key.ALIAS, ""));
                        mTvRequest.setText(requestJson);
                        action = "HeartBeat";
                    }
                }));

        functions.put("Register",
                new Function("请输入要订阅的topic,多个频道使用','分隔开", "Register",
                        "{\"t\":2,\"topic\":[%s],\"num\":%d}", new Runnable() {
                    @Override
                    public void run() {
                        final Function heartBeat = functions.get("Register");
                        showSingleInputDialog(heartBeat.text, heartBeat.hint, new OnInputListener() {
                            @Override
                            public void onInputFinish(String msg) {
                                String trim = msg.trim();
                                while (trim.startsWith(",")) {
                                    trim = trim.substring(1);
                                    if (trim.length() == 0) return;
                                }

                                while (trim.endsWith(",")) {
                                    trim = trim.substring(0, trim.length() - 1);
                                    if (trim.length() == 0) return;
                                }
                                String[] split = trim.split(",");
                                StringBuilder sBuf = new StringBuilder();
                                int index = 0;
                                String[] topics = new String[split.length];
                                for (String str : split) {
                                    if (index > 0) {
                                        sBuf.append(",");
                                    }
                                    sBuf.append("\"").append(str).append("\"");
                                    topics[index] = str;
                                    index++;
                                }
                                String requestJson = String.format(heartBeat.requestJson, sBuf.toString(), System.currentTimeMillis());
                                mTvRequest.setText(requestJson);
                                action = "Register";
                                params.clear();
                                params.add(topics);
                            }
                        });
                    }
                }));

        functions.put("Get_Topic",
                new Function("", "Get_Topic",
                        "{\"t\":3,\"num\":%d}", new Runnable() {
                    @Override
                    public void run() {
                        Function get_topic = functions.get("Get_Topic");
                        String get_topic_requestJson = String.format(get_topic.requestJson, System.currentTimeMillis());
                        mTvRequest.setText(get_topic_requestJson);
                        action = "Get_Topic";
                        params.clear();
                    }
                }));

        functions.put("Pull",
                new Function("", "Pull",
                        "{\"t\":4,\"num\":%d}", new Runnable() {
                    @Override
                    public void run() {
                        Function get_topic = functions.get("Pull");
                        String get_topic_requestJson = String.format(get_topic.requestJson, System.currentTimeMillis());
                        mTvRequest.setText(get_topic_requestJson);
                        action = "Pull";
                        params.clear();
                    }
                }));

        functions.put("Push-To-Alias",
                new Function("", "Push-To-Alias", push_to_alias,
                        new Runnable() {
                            @Override
                            public void run() {
                                final Function function = functions.get("Push-To-Alias");
                                showPushInputDialog(function.text,
                                        new String[]{"请输入目标alias",
                                                "请输入要发送的消息",
                                                "请输入消息存活时间"},
                                        new OnInputsListener() {
                                            @Override
                                            public void onInputFinish(String[] msg) {
                                                if(!(msg[2].matches("[\\d]+"))){
                                                    Toast.makeText(MainActivity.this, "存活时间必须为数字,单位分钟", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                String format = String.format(function.requestJson,
                                                        msg[0], msg[1],
                                                        Integer.parseInt(msg[2]),
                                                        System.currentTimeMillis());
                                                mTvRequest.setText(format);
                                                action = "Push-To-Alias";
                                                params.clear();
                                                params.add(msg[0]);
                                                params.add(msg[1]);
                                                params.add(Integer.parseInt(msg[2]));
                                            }
                                        });
                            }
                        }
                ));
    }

    public void showPushInputDialog(String title, final String[] hint, final OnInputsListener listener) {
        final LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout.LayoutParams editTextLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        float v = Engine.dp2px(this, 2);
        editTextLayoutParams.rightMargin = (int) v;
        editTextLayoutParams.leftMargin = (int) v;
        editTextLayoutParams.topMargin = (int) v;
        editTextLayoutParams.bottomMargin = (int) v;
        int index = 0;
        for (String hintStr : hint) {
            EditText editText = new EditText(this);
            editText.setHint(hintStr);
            editText.setLayoutParams(editTextLayoutParams);
            linearLayout.addView(editText, index++);
        }

        mInputsDialog = new MaterialDialog(this)
                .setTitle(title)
                .setContentView(linearLayout)
                .setPositiveButton("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[] strings = new String[hint.length];
                        for (int i = 0; i < hint.length; i++) {
                            EditText et = (EditText) linearLayout.getChildAt(i);
                            String s = et.getText().toString();
                            Log.e(TAG, "onClick: inputs: "+s);
                            if (TextUtils.isEmpty(s)) {
                                Toast.makeText(MainActivity.this,
                                        et.getHint().toString() + "", Toast.LENGTH_SHORT).show();
                                return;
                            } else {
                                strings[i] = s;
                            }
                        }
                        if (listener != null) {
                            listener.onInputFinish(strings);
                        }
                        mInputsDialog.dismiss();
                    }
                })
                .setNegativeButton("CANCLE", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mInputsDialog.dismiss();
                    }
                });
        mInputsDialog.show();
    }

    private void showSingleInputDialog(String title, String hint, final OnInputListener listener) {
        final EditText editText = new EditText(this);
        editText.setHint(hint);
        materialDialog = new MaterialDialog(this)
                .setTitle(title)
                .setContentView(editText)
                .setPositiveButton("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String s = editText.getText().toString();
                        if (TextUtils.isEmpty(s)) {
                            Toast.makeText(MainActivity.this, "值不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        listener.onInputFinish(s);
                        materialDialog.dismiss();
                    }
                })
                .setNegativeButton("CANCLE", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        materialDialog.dismiss();
                    }
                });
        materialDialog.show();
    }

    private void findView() {
        root = View.inflate(this, R.layout.dialog_setting, null);

        mLvTopics = (ListView) root.findViewById(R.id.main_lv_topics);
        mEtAlias = (EditText) root.findViewById(R.id.main_et_alias);
        mEtHeartbeat = (EditText) root.findViewById(R.id.main_et_heartbeat);
        mEtTopics = (EditText) root.findViewById(R.id.main_et_topic);
        mLlTopic = (LinearLayout) root.findViewById(R.id.main_ll_topics);
        objects = new ArrayList<>();
        final Set<String> topics = SP.getSet(Key.TOPICS);
        if (topics != null) {
            objects.addAll(topics);
            // adapter.notifyDataSetChanged();
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, objects);
        mLvTopics.setAdapter(adapter);

        mHSV = (HorizontalScrollView) findViewById(R.id.main_sv_function);
        mLlContainer = (LinearLayout) findViewById(R.id.main_ll_funs);
        mTvRequest = (TextView) findViewById(R.id.main_tv_request);
        mTvResponse = (TextView) findViewById(R.id.main_tv_response);
        mTvPush = (TextView) findViewById(R.id.main_tv_rpush);
        fillFunctions();
        for (String s : functions.keySet()) {
            Button heartbeat = createFunctionBtn(s);
            heartbeat.setOnClickListener(clickListener);
            mLlContainer.addView(heartbeat);
        }
    }


    private Button createFunctionBtn(String text) {
        Button button = new Button(this);

        LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        float v = Engine.dp2px(this, 2);
        layoutParam.rightMargin = (int) v;
        layoutParam.leftMargin = (int) v;
        layoutParam.topMargin = (int) v;
        layoutParam.bottomMargin = (int) v;
        button.setLayoutParams(layoutParam);
        button.setText(text);
        button.setTag(text);
        return button;
    }

    private void showDialog(String title) {
        final String alias = SP.get(Key.ALIAS, "");
        String heartbeat = SP.get(Key.HEARTBEAT, "10");
        final Set<String> topics = SP.getSet(Key.TOPICS);
        mEtAlias.setText(alias);

        mEtHeartbeat.setText(heartbeat);
        if (objects.size() == 0) mLlTopic.setVisibility(View.GONE);
        else mLlTopic.setVisibility(View.VISIBLE);
        if (topics != null) {
            objects.clear();
            objects.addAll(topics);
            adapter.notifyDataSetChanged();
        }
        mMaterialDialog = new MaterialDialog(this)
                .setTitle(title)
                .setContentView(root)
                .setPositiveButton("SAVE", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text2 = mEtAlias.getText().toString();
                        if (TextUtils.isEmpty(text2)) {
                            if (TextUtils.isEmpty(alias)) {
                                Toast.makeText(getApplicationContext(), "请设置alias", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            SP.put(Key.ALIAS, text2);
                        }

                        String newtopics = mEtTopics.getText().toString();
                        if (!TextUtils.isEmpty(newtopics)) {
                            Set<String> setStr;
                            if (topics != null) {
                                setStr = topics;
                            } else {
                                setStr = new HashSet<String>();

                            }
                            setStr.add(newtopics);
                            SP.putSet(Key.TOPICS, setStr);
                        }

                        String newHeartbeat = mEtHeartbeat.getText().toString();
                        if (!TextUtils.isEmpty(newHeartbeat)) {
                            SP.put(Key.HEARTBEAT, newHeartbeat);
                        }

                        mMaterialDialog.dismiss();

                    }
                })
                .setNegativeButton("CANCEL", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(alias)) {
                            Toast.makeText(getApplicationContext(), "请设置alias", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mMaterialDialog.dismiss();

                    }
                });

        mMaterialDialog.show();
    }

    public void switchPush(View view) {
        if (ZZWPush.isStart()) {
            ZZWPush.stop();
        } else {
            String alias = SP.get(Key.ALIAS, "");
            Set<String> topics = SP.getSet(Key.TOPICS);
            if (topics == null) {
                topics = new TreeSet<String>();
            }
            ZZWPush.startWithAliasAndTopics(alias,
                    topics.toArray(new String[0]),
                    null,
                    new IPushClient.PushObserver() {
                        @Override
                        public void onReback(@NonNull PushResult pushResult) {
                            Toast.makeText(getApplicationContext(), "startPush: " + pushResult.success, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String key = (String) v.getTag();
            Function function = functions.get(key);
            Log.e(TAG, "onClick: " + key + ", " + function);
            if (function != null && function.selectRunnable != null) {
                Log.e(TAG, "onClick: 2" + function.requestJson);
                function.selectRunnable.run();
            }
        }
    };

    public void clickSend(View view) {
        if (action == null) {
            Toast.makeText(this, "请先选择功能", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (action){
            case "HeartBeat":

                break;
            case "Register":
                String[] o = (String[]) params.get(0);
                ZZWPush.subscribe(o, new IPushClient.PushObserver() {
                    @Override
                    public void onReback(@NonNull PushResult pushResult) {
                        mTvResponse.setText("success: "+pushResult.success+", msg: "+pushResult.msg);
                    }
                },null);
                break;
            case "Get_Topic":
                ZZWPush.getTopics(new IPushClient.PushObserver() {
                    @Override
                    public void onReback(@NonNull PushResult pushResult) {
                        mTvResponse.setText("success: "+pushResult.success+", msg: "+pushResult.msg);
                    }
                });
                break;
            case "Pull":
                ZZWPush.pullMsg(new IPushClient.PushObserver() {
                    @Override
                    public void onReback(@NonNull PushResult pushResult) {
                        mTvResponse.setText("success: "+pushResult.success+", msg: "+pushResult.msg);
                    }
                });
                break;
            case "Push-To-Alias":
                String alias  = (String) params.get(0);
                String msg  = (String) params.get(1);
                int livetime  = (int) params.get(2);
                ZZWPush.pushMsg2Alias(new String[]{alias}, msg, livetime, new IPushClient.PushObserver() {
                    @Override
                    public void onReback(@NonNull PushResult pushResult) {
                        mTvResponse.setText("success: "+pushResult.success+", msg: "+pushResult.msg);
                    }
                });
                break;

        }
    }

    interface OnInputListener {
        void onInputFinish(String msg);
    }

    interface OnInputsListener {
        void onInputFinish(String[] msg);
    }

    private ZPushReceiver mPushReceiver = new ZPushReceiver();

    class ZPushReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ZZWPush.ACTION.PUSH_ARRIVE:
                    // in
                    String pushArrive = intent.getStringExtra(ZZWPush.KEY.PUSH_ARRIVE);
                    mTvPush.setText("收到推送: "+pushArrive);
                    break;
            }
        }
    }
}
