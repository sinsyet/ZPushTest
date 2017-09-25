package com.example.zpushtest.utils;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

public class SP {

    private static SharedPreferences sp;

    public static void init(Context ctx){
        if(ctx == null) throw new IllegalStateException("ctx can't be null");
        sp = ctx.getSharedPreferences("appcache.xml", Context.MODE_PRIVATE);
    }
    public static String get(String key,String defaultValue){
        return sp.getString(key, defaultValue);
    }

    public static Set<String> getSet(String key){
        return sp.getStringSet(key, null);
    }
    public static void put(String key,String value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }
    public static void putSet(String key,Set<String> set){
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(key,set);
        editor.apply();
    }
}
