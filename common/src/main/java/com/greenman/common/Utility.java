package com.greenman.common;

public class Utility {
    public static final String PATH_DIGILOGUE_SETTINGS = "/digilogue/colours";

    // boolean keys
    public static final String KEY_12HOUR_FORMAT = "com.greenman.digilogue.12HOUR_FORMAT";
    public static final String KEY_WIDGET_SHOW_WEATHER = "com.greenman.digilogue.SHOW_WEATHER";
    public static final String KEY_WIDGET_WEATHER_FAHRENHEIT = "com.greenman.digilogue.FAHRENHEIT";
    public static final String KEY_WIDGET_WEATHER_AUTO_LOCATION = "com.greenman.digilogue.AUTO_LOCATION";

    // string keys
    public static final String KEY_BACKGROUND_COLOUR = "com.greenman.digilogue.BACKGROUND_COLOUR";
    public static final String KEY_MIDDLE_COLOUR = "com.greenman.digilogue.MIDDLE_COLOUR";
    public static final String KEY_FOREGROUND_COLOUR = "com.greenman.digilogue.FOREGROUND_COLOUR";
    public static final String KEY_ACCENT_COLOUR = "com.greenman.digilogue.ACCENT_COLOUR";
    public static final String KEY_WIDGET_WEATHER_LOCATION = "com.greenman.digilogue.LOCATION";

    // int keys
    public static final String KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C = "com.greenman.digilogue.WEATHER_TEMPERATURE_C";
    public static final String KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F = "com.greenman.digilogue.WEATHER_TEMPERATURE_F";
    public static final String KEY_WIDGET_WEATHER_DATA_CODE = "com.greenman.digilogue.WEATHER_CODE";

    // long keys
    public static final String KEY_WIDGET_WEATHER_DATA_DATETIME = "com.greenman.digilogue.WEATHER_DATETIME";

    // boolean defaults
    public static final boolean CONFIG_12HOUR_DEFAULT = false;
    public static final boolean CONFIG_WIDGET_SHOW_WEATHER_DEFAULT = false;
    public static final boolean CONFIG_WIDGET_FAHRENHEIT_DEFAULT = false;
    public static final boolean CONFIG_AUTO_LOCATION_DEFAULT = true;

    // string defaults
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "black";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE = "gray";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND = "white";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT = "red";
    public static final String CONFIG_LOCATION_DEFAULT = "";

    // int defaults
    public static int WIDGET_WEATHER_DATA_TEMPERATURE_C_DEFAULT = -999;
    public static int WIDGET_WEATHER_DATA_TEMPERATURE_F_DEFAULT = -999;
    public static int WIDGET_WEATHER_DATA_CODE_DEFAULT = Utility.WeatherCodes.UNKNOWN;

    public static class WeatherCodes {
        public static final int UNKNOWN = -1;
        public static final int SUNNY = 113;
        public static final int PARTLY_CLOUDY = 116;
    }
}
