package com.greenman.common;

public class Utility {
    public static final String PATH_DIGILOGUE_SETTINGS = "/digilogue/colours";

    // boolean keys
    public static final String KEY_12HOUR_FORMAT = "com.greenman.digilogue.12HOUR_FORMAT";
    public static final String KEY_WIDGET_SHOW_WEATHER = "com.greenman.digilogue.SHOW_WEATHER";
    public static final String KEY_WIDGET_WEATHER_FAHRENHEIT = "com.greenman.digilogue.FAHRENHEIT";
    public static final String KEY_WIDGET_WEATHER_AUTO_LOCATION = "com.greenman.digilogue.AUTO_LOCATION";
    public static final String KEY_WIDGET_WEATHER_DATA_ISDAYTIME = "com.greenman.digilogue.WEATHER_ISDAYTIME";

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
    public static final boolean CONFIG_WIDGET_WEATHER_DAYTIME_DEFAULT = true;

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
        public static final int CLOUDY = 119;
        public static final int OVERCAST = 122;
        public static final int MIST = 143;
        public static final int PATCHY_RAIN_NEARBY = 176;
        public static final int PATCHY_SNOW_NEARBY = 179;
        public static final int PATCHY_SLEET_NEARBY = 182;
        public static final int PATCHY_FREEZING_DRIZZLE_NEARBY = 185;
        public static final int THUNDERY_OUTBREAKS = 200;
        public static final int BLOWING_SNOW = 227;
        public static final int BLIZZARD = 230;
        public static final int FOG = 248;
        public static final int FREEZING_FOG = 260;
        public static final int PATCHY_LIGHT_DRIZZLE = 263;
        public static final int LIGHT_DRIZZLE = 266;
        public static final int FREEZING_DRIZZLE = 281;
        public static final int HEAVY_FREEZING_DRIZZLE = 284;
        public static final int PATCHY_LIGHT_RAIN = 293;
        public static final int LIGHT_RAIN = 296;
        public static final int MODERATE_RAIN_AT_TIMES = 299;
        public static final int MODERATE_RAIN = 302;
        public static final int HEAVY_RAIN_AT_TIMES = 305;
        public static final int HEAVY_RAIN = 308;
        public static final int LIGHT_FREEZING_RAIN = 311;
        public static final int MODERATE_OR_HEAVY_FREEZING_RAIN = 314;
        public static final int LIGHT_SLEET = 317;
        public static final int MODERATE_OR_HEAVY_SLEET = 320;
        public static final int PATCHY_LIGHT_SNOW = 323;
        public static final int LIGHT_SNOW = 326;
        public static final int PATCHY_MODERATE_SNOW = 329;
        public static final int MODERATE_SNOW = 329;
        public static final int PATCHY_HEAVY_SNOW = 335;
        public static final int HEAVY_SNOW = 338;
        public static final int ICE_PELLETS = 350;
        public static final int LIGHT_RAIN_SHOWER = 353;
        public static final int MODERATE_OR_HEAVY_RAIN_SHOWER = 356;
        public static final int TORRENTIAL_RAIN_SHOWER = 359;
        public static final int LIGHT_SLEET_SHOWERS = 362;
        public static final int MODERATE_OR_HEAVY_SLEET_SHOWERS = 365;
        public static final int LIGHT_SNOW_SHOWERS = 368;
        public static final int MODERATE_OR_HEAVY_SNOW_SHOWERS = 371;
        public static final int LIGHT_SHOWERS_OF_ICE_PELLETS = 374;
        public static final int MODERATE_OR_HEAVY_SHOWERS_OF_ICE_PELLETS = 377;
        public static final int PATCHY_LIGHT_RAIN_IN_AREA_WITH_THUNDER = 386;
        public static final int MODERATE_OR_HEAVY_RAIN_IN_AREA_WITH_THUNDER = 389;
        public static final int PATCHY_LIGHT_SNOW_IN_AREA_WITH_THUNDER = 392;
        public static final int MODERATE_OR_HEAVY_SNOW_IN_AREA_WITH_THUNDER = 395;
    }
}
