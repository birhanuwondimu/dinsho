package com.dinsho.solo;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.dinsho.solo.Model.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class Utils {
    public static Constants getConstants(String name)
            throws JsonSyntaxException, JsonIOException, FileNotFoundException {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(new FileReader("src/Constants.json"), JsonObject.class);
        return gson.fromJson(jsonObject.getAsJsonObject(name), Constants.class);
    }

    public static int getNumberFromString(String str) {
        if (NullOrEmpty(str))
            return 0;
        return Integer.parseInt(str.replaceAll("[^0-9]", ""));
    }

    public static boolean NullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

}
