package sample.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import sample.model.TimeSeriesDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ESP32ServiceImpl implements ESP32Service {

    private ObjectMapper objectMapper;

    public ESP32ServiceImpl() {
    }

    public ESP32ServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    public boolean healthy(String ip) {
        URL url = new URL("http://" + ip + "/alive");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return String.valueOf(content).equals("I'm alive");
    }

    @Override
    @SneakyThrows
    public List<TimeSeriesDTO> getData(String ip) {
        URL url = new URL("http://" + ip + "/data");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return objectMapper.readValue(content.toString(), new TypeReference<List<TimeSeriesDTO>>(){});
    }
}
