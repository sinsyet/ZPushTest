package com.example.zpushtest.bean;


public class Function {
    public String hint;                 // 提示语
    public String text;                 // button显示的文本
    public String requestJson;          // 请求json
    public Runnable selectRunnable;     // 被选中后执行

    public Function() {
    }

    public Function(String hint,
                    String text,
                    String requestJson,
                    Runnable selectRunnable) {
        this.hint = hint;
        this.text = text;
        this.requestJson = requestJson;
        this.selectRunnable = selectRunnable;
    }

    public String toRequestJson() {
        return "";
    }
}
