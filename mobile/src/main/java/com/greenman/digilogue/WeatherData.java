package com.greenman.digilogue;


import com.greenman.common.Utility;

import org.json.JSONException;
import org.json.JSONObject;

class WeatherData {
    // Defaults
    private int temperatureC = -999;
    private int temperatureF = -999;
    private int code = Utility.WeatherCodes.UNKNOWN;
    private String location = "";
    private boolean dayTime = true;

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
    public boolean isDayTime() {
        return dayTime;
    }

    public WeatherData(String jsonString) throws JSONException {
        if (!jsonString.contains("error")) {
            // parse json
            JSONObject jObj = new JSONObject(jsonString);

            JSONObject dataObj = jObj.getJSONObject("data");
            JSONObject currentConditionObj = (JSONObject) dataObj.getJSONArray("current_condition").get(0);
            /*temperatureC = Integer.parseInt(currentConditionObj.getString("temp_C"));
            temperatureF = Integer.parseInt(currentConditionObj.getString("temp_F"));*/
            temperatureC = Integer.parseInt(currentConditionObj.getString("FeelsLikeC"));
            temperatureF = Integer.parseInt(currentConditionObj.getString("FeelsLikeF"));
            code = Integer.parseInt(currentConditionObj.getString("weatherCode"));
            dayTime = currentConditionObj.getString("isdaytime").equals("yes");

            JSONObject requestObj = (JSONObject) dataObj.getJSONArray("request").get(0);
            location = requestObj.getString("query");
        }
    }
}
