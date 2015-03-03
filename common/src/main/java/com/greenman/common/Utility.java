package com.greenman.common;

public class Utility {
    public static final String PATH_DIGILOGUE_SETTINGS = "/digilogue/colours";
    public static final int REFRESH_WEATHER_DELAY_HOURS = 3;

    // boolean keys
    public static final String KEY_TOGGLE_AM_PM = "com.greenman.digilogue.12HOUR_FORMAT";
    public static final String KEY_TOGGLE_ANALOGUE = "com.greenman.digilogue.ANALOGUE";
    public static final String KEY_TOGGLE_DRAW_DIAL = "com.greenman.digilogue.DRAW_DIAL";
    public static final String KEY_TOGGLE_DIGITAL = "com.greenman.digilogue.DIGITAL";
    public static final String KEY_TOGGLE_BATTERY = "com.greenman.digilogue.BATTERY";
    public static final String KEY_TOGGLE_DAY_DATE = "com.greenman.digilogue.DAY_DATE";
    public static final String KEY_TOGGLE_DIM_COLOUR = "com.greenman.digilogue.DIM_COLOUR";
    public static final String KEY_TOGGLE_SOLID_TEXT = "com.greenman.digilogue.SOLID_TEXT";
    public static final String KEY_TOGGLE_WEATHER = "com.greenman.digilogue.SHOW_WEATHER";
    public static final String KEY_TOGGLE_FIX_CHIN = "com.greenman.digilogue.FIX_CHIN";

    public static final String KEY_WIDGET_WEATHER_FAHRENHEIT = "com.greenman.digilogue.FAHRENHEIT";
    public static final String KEY_WIDGET_WEATHER_AUTO_LOCATION = "com.greenman.digilogue.AUTO_LOCATION";
    public static final String KEY_WIDGET_WEATHER_DATA_ISDAYTIME = "com.greenman.digilogue.WEATHER_ISDAYTIME";

    public static final String KEY_ANALOGUE_ELEMENT_SIZE = "com.greenman.digilogue.ANALOGUE_ELEMENT_SIZE";
    public static final String KEY_DIGITAL_ELEMENT_SIZE = "com.greenman.digilogue.DIGITAL_ELEMENT_SIZE";

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
    public static final boolean CONFIG_DEFAULT_TOGGLE_AM_PM = false;
    public static final boolean CONFIG_DEFAULT_TOGGLE_ANALOGUE = true;
    public static final boolean CONFIG_DEFAULT_TOGGLE_DIAL = false;
    public static final boolean CONFIG_DEFAULT_TOGGLE_DIGITAL = true;
    public static final boolean CONFIG_DEFAULT_TOGGLE_BATTERY = true;
    public static final boolean CONFIG_DEFAULT_TOGGLE_DAY_DATE = true;
    public static final boolean CONFIG_DEFAULT_TOGGLE_DIM_COLOUR = true;
    public static final boolean CONFIG_DEFAULT_TOGGLE_SOLID_TEXT = false;
    public static final boolean CONFIG_DEFAULT_TOGGLE_FIX_CHIN = false;

    public static final boolean CONFIG_DEFAULT_TOGGLE_WEATHER = false;
    public static final boolean CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT = false;
    public static final boolean CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION = true;
    public static final boolean CONFIG_DEFAULT_WIDGET_WEATHER_DAYTIME = true;

    // string defaults
    public static final String COLOUR_NAME_DEFAULT_BACKGROUND = "black";
    public static final String COLOUR_NAME_DEFAULT_MIDDLE = "gray";
    public static final String COLOUR_NAME_DEFAULT_FOREGROUND = "white";
    public static final String COLOUR_NAME_DEFAULT_ACCENT = "red";
    public static final String CONFIG_DEFAULT_WIDGET_WEATHER_LOCATION = "";

    // int defaults
    public static final int WIDGET_WEATHER_DATA_DEFAULT_TEMPERATURE_C = -999;
    public static final int WIDGET_WEATHER_DATA_DEFAULT_TEMPERATURE_F = -999;
    public static final int WIDGET_WEATHER_DATA_DEFAULT_CODE = Utility.WeatherCodes.UNKNOWN;

    // long defaults
    public static final long WIDGET_WEATHER_DATA_DEFAULT_DATETIME = 0;

    public static class WeatherCodes {
        public static final int UNKNOWN = -1;
        public static final int CLEAR = 113;
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
        public static final int MODERATE_SNOW = 332;
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

        public static int getStringResourceByCode(int code) {
            switch (code) {
                case Utility.WeatherCodes.CLEAR:
                    return R.string.clear;
                case Utility.WeatherCodes.PARTLY_CLOUDY:
                    return R.string.partly_cloudy;
                case Utility.WeatherCodes.CLOUDY:
                case Utility.WeatherCodes.OVERCAST:
                    return R.string.cloudy;
                case Utility.WeatherCodes.MIST:
                case Utility.WeatherCodes.FOG:
                case Utility.WeatherCodes.FREEZING_FOG:
                    return R.string.mist;
                case Utility.WeatherCodes.PATCHY_RAIN_NEARBY:
                case Utility.WeatherCodes.PATCHY_LIGHT_DRIZZLE:
                case Utility.WeatherCodes.LIGHT_RAIN_SHOWER:
                    return R.string.patchy_rain;
                case Utility.WeatherCodes.PATCHY_SNOW_NEARBY:
                case Utility.WeatherCodes.LIGHT_SLEET_SHOWERS:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_SLEET_SHOWERS:
                case Utility.WeatherCodes.LIGHT_SHOWERS_OF_ICE_PELLETS:
                    return R.string.patchy_snow;
                case Utility.WeatherCodes.PATCHY_SLEET_NEARBY:
                case Utility.WeatherCodes.PATCHY_FREEZING_DRIZZLE_NEARBY:
                case Utility.WeatherCodes.FREEZING_DRIZZLE:
                case Utility.WeatherCodes.HEAVY_FREEZING_DRIZZLE:
                case Utility.WeatherCodes.LIGHT_FREEZING_RAIN:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_FREEZING_RAIN:
                case Utility.WeatherCodes.LIGHT_SLEET:
                case Utility.WeatherCodes.ICE_PELLETS:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_SHOWERS_OF_ICE_PELLETS:
                    return R.string.patchy_sleet;
                case Utility.WeatherCodes.THUNDERY_OUTBREAKS:
                case Utility.WeatherCodes.PATCHY_LIGHT_RAIN_IN_AREA_WITH_THUNDER:
                case Utility.WeatherCodes.PATCHY_LIGHT_SNOW_IN_AREA_WITH_THUNDER:
                    return R.string.thundery_outbreaks;
                case Utility.WeatherCodes.BLOWING_SNOW:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_SLEET:
                    return R.string.blowing_snow;
                case Utility.WeatherCodes.BLIZZARD:
                case Utility.WeatherCodes.PATCHY_MODERATE_SNOW:
                case Utility.WeatherCodes.MODERATE_SNOW:
                case Utility.WeatherCodes.HEAVY_SNOW:
                    return R.string.blizzard;
                case Utility.WeatherCodes.LIGHT_DRIZZLE:
                case Utility.WeatherCodes.PATCHY_LIGHT_RAIN:
                case Utility.WeatherCodes.LIGHT_RAIN:
                    return R.string.light_drizzle;
                case Utility.WeatherCodes.MODERATE_RAIN_AT_TIMES:
                case Utility.WeatherCodes.HEAVY_RAIN_AT_TIMES:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_RAIN_SHOWER:
                    return R.string.moderate_rain_at_times;
                case Utility.WeatherCodes.MODERATE_RAIN:
                case Utility.WeatherCodes.HEAVY_RAIN:
                case Utility.WeatherCodes.TORRENTIAL_RAIN_SHOWER:
                    return R.string.moderate_rain;
                case Utility.WeatherCodes.PATCHY_LIGHT_SNOW:
                case Utility.WeatherCodes.LIGHT_SNOW:
                case Utility.WeatherCodes.LIGHT_SNOW_SHOWERS:
                    return R.string.patchy_light_snow;
                case Utility.WeatherCodes.PATCHY_HEAVY_SNOW:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_SNOW_SHOWERS:
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_SNOW_IN_AREA_WITH_THUNDER:
                    return R.string.patchy_heavy_snow;
                case Utility.WeatherCodes.MODERATE_OR_HEAVY_RAIN_IN_AREA_WITH_THUNDER:
                    return R.string.moderate_rain_with_thunder;
                default:
                    return R.string.unknown;
            }
        }
    }
}
