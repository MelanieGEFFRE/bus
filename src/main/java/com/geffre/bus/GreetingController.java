package com.geffre.bus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class GreetingController {

    private static final String apiUrl = "https://api.tisseo.fr/v1/";
    private static final String jsonFormat = "json";
    private static final String keyParam = "key=39ed22b9-4498-41d9-ad77-9e2df5956513";
    private static final String lineService = "lines";
    private static final String stopPointService = "stop_points";
    private static final String horaireService = "stops_schedules";
    private static final SimpleDateFormat tisseoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/getNextPassage")
    public String getNextPassage(@RequestParam(value = "nameLine") String nameLine, @RequestParam("stop") String requestedStop, @RequestParam("finalDestination") String finaleDestination) throws IOException, ParseException {
        String lineId = getLineId(nameLine);
        String stopId = getStopId(lineId, requestedStop, finaleDestination);
        String urlSchedul = apiUrl+horaireService+"."+jsonFormat+"?"+
                "stopPointId="+stopId+"&"+"lineId="+lineId+"&"+
                keyParam;
        URL url = new URL(urlSchedul);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        JsonObject jsonLine = new JsonParser().parse(content.toString()).getAsJsonObject();
        String nextDate = jsonLine.getAsJsonObject("departures").getAsJsonArray("departure").get(0).getAsJsonObject().get("dateTime").getAsString();
        Date actualDate = new Date();
        String actual = tisseoDateFormat.format(new Date());
        String[] nextDateTime = nextDate.split(" ")[1].split(":");
        String[] actualDateTime = actual.split(" ")[1].split(":");
        long nextTime = Long.parseLong(nextDateTime[2])*1000+
                Long.parseLong(nextDateTime[1])*1000*60+
                Long.parseLong(nextDateTime[0])*1000*60*60;
        long actualTime = Long.parseLong(actualDateTime[2])*1000+
                Long.parseLong(actualDateTime[1])*1000*60+
                Long.parseLong(actualDateTime[0])*1000*60*60;
        long diff = nextTime-actualTime;
        if (diff < 0){
            diff = nextTime+(24*1000*60*60)-actualTime;
        }
        if (diff / 1000 < 60) {
            return "<1mn";
        } else if (diff/(60*1000) < 120 && diff/(60*1000) >= 1){
            return Long.toString(diff/(60*1000));
        } else {
            return ">2h";
        }
    }

    private String getStopId(String lineId, String requestedStop, String finaleDestination) throws IOException {
        String urlLines = apiUrl+stopPointService+"."+jsonFormat+
        "?"+"lineId="+lineId+"&"+"displayDestinations=1"+"&"+
        keyParam;
//"http://api.tisseo.fr/v1/stop_points.json?lineId=11821953316814883&displayDestinations=1&key=39ed22b9-4498-41d9-ad77-9e2df5956513";
        URL url = new URL(urlLines);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        JsonObject jsonLine = new JsonParser().parse(content.toString()).getAsJsonObject();
        JsonArray stopList = jsonLine.getAsJsonObject("physicalStops").getAsJsonArray("physicalStop");
        for (JsonElement jsonElem : stopList){
            if (jsonElem.getAsJsonObject().get("name").getAsString().equals(requestedStop)){
                for (JsonElement destination : jsonElem.getAsJsonObject().getAsJsonArray("destinations")) {
                    if (destination.getAsJsonObject().get("name").getAsString().equals(finaleDestination)) {
                        return jsonElem.getAsJsonObject().get("id").getAsString();
                    }
                }
            }
        }
        return "error in getting stop id";

    }

    private String getLineId(String nameLine) throws IOException {
        String urlLines = apiUrl+lineService+"."+jsonFormat+"?"+"shortName="+nameLine+"&"+keyParam;
        //Récupération id ligne
        URL url = new URL(urlLines);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        JsonObject jsonLine = new JsonParser().parse(content.toString()).getAsJsonObject();
        return jsonLine.getAsJsonObject("lines").getAsJsonArray("line").get(0).getAsJsonObject().get("id").getAsString();
    }
}
