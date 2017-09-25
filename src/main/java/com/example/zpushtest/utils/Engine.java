package com.example.zpushtest.utils;


import android.app.Activity;
import android.content.Context;
import android.view.Display;

public class Engine {

    public static float dp2px(Activity aty, float dp) {
        final float scale = aty.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

}
