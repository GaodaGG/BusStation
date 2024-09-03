package com.gg.busStation.function;

import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.gg.busStation.data.bus.ETA;
import com.gg.busStation.data.bus.Route;
import com.gg.busStation.data.bus.Stop;
import com.gg.busStation.function.internet.HttpClientHelper;
import com.gg.busStation.function.internet.JsonToBean;
import com.gg.busStation.function.location.LocationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private static long lastUpdateTime = 0;

    private DataManager() {
    }

    public static void initData() throws IOException {
        //判断是否需要更新数据
        Map<String, String> settings = DataBaseManager.getSettings();
        String oldLastUpdateTime = settings.get("lastUpdateTime");
        lastUpdateTime = Long.parseLong(oldLastUpdateTime);

        if (System.currentTimeMillis() <= lastUpdateTime + Long.parseLong(settings.get("updateTime")) && "true".equals(settings.get("isInit"))) {
            return;
        }

        List<Route> routeList = initRoutes();
        List<Stop> stopList = initStops();

        DataBaseManager.initData(routeList, stopList);
    }

    public static List<Route> initRoutes() throws IOException {
        List<Route> routes = new ArrayList<>();

        // 获取九巴路线列表
        KMB.initRoutes(routes);

        // 获取城巴路线列表
        CTB.initRoutes(routes);

        return routes;
    }

    public static List<Stop> initStops() throws IOException {
        List<Stop> stops = new ArrayList<>();

        // 获取九巴站点数据
        String data = HttpClientHelper.getData(KMB.stopUrl);
        for (JsonElement jsonElement : JsonToBean.extractJsonArray(data)) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            stops.add(JsonToBean.jsonToStop(jsonObject));
        }
        return stops;
    }

    public static List<ETA> routeAndStopToETAs(Route route, Stop stop, int seq) throws IOException {
        List<ETA> etas = new ArrayList<>();
        KMB.routeAndStopToETAs(route, stop, etas);
        CTB.routeAndStopToETAs(route, seq, etas);

        Collections.sort(etas, (eta1, eta2) -> {
            if (eta1.getEta() == null || eta2.getEta() == null) {
                return 0; // 处理空值（如果有）
            }
            return eta1.getEta().compareTo(eta2.getEta());
        });
        return etas;
    }

    public static List<Stop> routeToStops(Route route) throws IOException {
        List<Stop> itemStops = new ArrayList<>();

        if (Route.coKMB.equals(route.getCo())) {
            return KMB.routeToStops(route, itemStops);
        }

//        String url = CTB.routeToStopUrl + route.getRoute() + "/" + route.getBound();
//        String data = HttpClientHelper.getData(url);
//        for (JsonElement jsonElement : JsonToBean.extractJsonArray(data)) {
//
//        }


        return itemStops;
    }

    public static int findNearestStopIndex(List<Stop> stops, LatLng location) {
        double minDistance = Double.MAX_VALUE;
        int nearestIndex = -1;

        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            LatLng stopLocation = new LatLng(Double.parseDouble(stop.getLat()), Double.parseDouble(stop.getLong()));

            double distance = LocationHelper.distance(location, stopLocation);

            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return nearestIndex;
    }

    public static long getMinutesRemaining(Date targetDate) {
        Date now = new Date();
        long currentTimeMillis = now.getTime();
        long targetTimeMillis = targetDate.getTime();
        long timeDifferenceMillis = targetTimeMillis - currentTimeMillis;

        // 将毫秒差转换为分钟
        long minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeDifferenceMillis);

        return minutesRemaining;
    }

    private static class KMB {
        public static final String routeUrl = "https://data.etabus.gov.hk/v1/transport/kmb/route/";
        public static final String stopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/stop/";
        public static final String routeToStopUrl = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/";
        public static final String routeAndStopToETAUrl = "https://data.etabus.gov.hk/v1/transport/kmb/eta/";

        private static void initRoutes(List<Route> routes) throws IOException {
            String kmbData = HttpClientHelper.getData(KMB.routeUrl);
            for (JsonElement jsonElement : JsonToBean.extractJsonArray(kmbData)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Route route = JsonToBean.jsonToRoute(jsonObject);
                route.setCo(Route.coKMB);
                routes.add(route);
            }
        }

        public static List<Stop> routeToStops(Route route, List<Stop> itemStops) throws IOException {
            String url = KMB.routeToStopUrl + route.getRoute() + "/" + route.getBound() + "/" + route.getService_type();
            String data = HttpClientHelper.getData(url);
            for (JsonElement jsonElement : JsonToBean.extractJsonArray(data)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String stopId = jsonObject.get("stop").getAsString();

                itemStops.add(DataBaseManager.findStop(stopId));
            }
            return itemStops;
        }

        public static void routeAndStopToETAs(Route route, Stop stop, List<ETA> etas) throws IOException {
            String url = KMB.routeAndStopToETAUrl + stop.getStop() + "/" + route.getRoute() + "/" + route.getService_type();
            String data = HttpClientHelper.getData(url);

            for (JsonElement jsonElement : JsonToBean.extractJsonArray(data)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                ETA eta = JsonToBean.jsonToETA(jsonObject);
                if (eta.getEta() != null) etas.add(eta);
            }
        }
    }

    private static class CTB {
        public static final String routeUrl = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb";
        public static final String stopUrl = "https://rt.data.gov.hk/v2/transport/citybus/stop/";
        public static final String routeToStopUrl = "https://rt.data.gov.hk/v2/transport/citybus/route-stop/ctb/";
        public static final String routeAndStopToETAUrl = "https://rt.data.gov.hk/v2/transport/citybus/eta/ctb/";

        public static void initRoutes(List<Route> routes) throws IOException {
            String ctbData = HttpClientHelper.getData(CTB.routeUrl);
            for (JsonElement jsonElement : JsonToBean.extractJsonArray(ctbData)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Route route = JsonToBean.jsonToRoute(jsonObject);
                route.setBound(Route.In);
                route.setService_type("1");
                routes.add(route);
            }
        }

        public static void routeAndStopToETAs(Route route, int seq, List<ETA> etas) throws IOException {
            String stopUrl = CTB.routeToStopUrl + route.getRoute() + "/" + (Route.In.equals(route.getBound()) ? Route.Out : Route.In);
            String stopData = HttpClientHelper.getData(stopUrl);
            JsonArray jsonElements = JsonToBean.extractJsonArray(stopData);
            String stop = jsonElements.get(seq - 1).getAsJsonObject().get("stop").getAsString();

            String url = CTB.routeAndStopToETAUrl + stop + "/" + route.getRoute();
            String data = HttpClientHelper.getData(url);

            for (JsonElement jsonElement : JsonToBean.extractJsonArray(data)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (!"".equals(jsonObject.get("eta").getAsString())){
                    ETA eta = JsonToBean.jsonToETA(jsonObject);
                    etas.add(eta);
                }
            }
        }
    }
}
