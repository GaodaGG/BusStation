package com.gg.busStation.data.database;

public class SQLConstants {
    private SQLConstants() {
    }

    public static final String routeDBName = "routes";
    public static final String stopDBName = "stops";
    public static final String routesHistoryDBName = "history";
    public static final String fareDBName = "fares";
    public static final String settingsDBName = "userSettings";

    public static final String createRouteDBCommand = "CREATE TABLE IF NOT EXISTS " + routeDBName + " (" +
            "co TEXT CHECK (co IN ('KMB', 'CTB', 'KMB+CTB')) NOT NULL," +
            "route TEXT NOT NULL," +
            "bound TEXT CHECK (bound IN ('outbound', 'inbound')) NOT NULL," +
            "service_type TEXT NOT NULL," +
            "orig_en TEXT NOT NULL," +
            "orig_tc TEXT NOT NULL," +
            "orig_sc TEXT NOT NULL," +
            "dest_en TEXT NOT NULL," +
            "dest_tc TEXT NOT NULL," +
            "dest_sc TEXT NOT NULL" +
            ");";

    public static final String createStopDBCommand = "CREATE TABLE IF NOT EXISTS " + stopDBName + " (" +
            "stop TEXT NOT NULL," +
            "name_en TEXT NOT NULL," +
            "name_tc TEXT NOT NULL," +
            "name_sc TEXT NOT NULL," +
            "lat REAL NOT NULL," +
            "long REAL NOT NULL" +
            ");";

    public static final String createRoutesHistoryDBCommand = "CREATE TABLE IF NOT EXISTS " + routesHistoryDBName + " (" +
            "co TEXT CHECK (co IN ('KMB', 'CTB', 'KMB+CTB')) NOT NULL," +
            "route TEXT NOT NULL," +
            "bound TEXT CHECK (bound IN ('outbound', 'inbound')) NOT NULL," +
            "service_type TEXT NOT NULL," +
            "timestamp INTEGER NOT NULL, " +
            "UNIQUE(route, bound, service_type)" +
            ");";

    public static final String createSettingsDBCommand = "CREATE TABLE IF NOT EXISTS " + settingsDBName + " (" +
            "key TEXT NOT NULL," +
            "value TEXT NOT NULL" +
            ");";

    public static final String createFareDBCommand = "CREATE TABLE IF NOT EXISTS " + fareDBName + " (" +
            "route TEXT NOT NULL," +
            "bound TEXT CHECK (bound IN ('outbound', 'inbound')) NOT NULL," +
            "service_type TEXT," +
            "fare TEXT NOT NULL" +
            ");";
}
