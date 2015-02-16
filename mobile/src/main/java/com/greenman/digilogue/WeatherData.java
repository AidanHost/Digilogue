package com.greenman.digilogue;


import com.greenman.common.Utility;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherData {
    // Defaults
    private int temperatureC = -999;
    private int temperatureF = -999;
    private int code = Utility.WeatherCodes.UNKNOWN;
    private String location = "";

    public int getTemperatureC() {
        return temperatureC;
    }

    public int getTemperatureF() {
        return temperatureF;
    }

    public int getCode() {
        return code;
    }

    public String getLocation() {
        return location;
    }

    public WeatherData(String jsonString) throws JSONException {
        if (!jsonString.contains("error")) {
            // parse json
            JSONObject jObj = new JSONObject(jsonString);

            JSONObject dataObj = jObj.getJSONObject("data");
            JSONObject currentConditionObj = (JSONObject) dataObj.getJSONArray("current_condition").get(0);
            temperatureC = Integer.parseInt(currentConditionObj.getString("temp_C"));
            temperatureF = Integer.parseInt(currentConditionObj.getString("temp_F"));
            code = Integer.parseInt(currentConditionObj.getString("weatherCode"));

            JSONObject requestObj = (JSONObject) dataObj.getJSONArray("request").get(0);
            location = requestObj.getString("query");
        }
    }
}
