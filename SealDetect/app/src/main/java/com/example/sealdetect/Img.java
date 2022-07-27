package com.example.sealdetect;

import org.json.JSONException;
import org.json.JSONObject;

public class Img {
    public String name;
    public boolean hasseal;

    public Img(){}
    public static Img jsonToObject(JSONObject json)
            throws JSONException {
        Img img = new Img();
        img.hasseal = json.getBoolean("hasseal");
        img.name = json.getString("name");
        return img;
    }
}
