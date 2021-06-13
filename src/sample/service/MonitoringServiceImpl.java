package sample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import sample.model.TimeSeriesDTO;
import sample.utils.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MonitoringServiceImpl implements MonitoringService {

    private static final String SSID = "A7";
    private static final String SSID_PASS = "hswn1561";
    private static final String FILE_NAME = "db.txt";
    private static final int DISTANCE_TO_SENSOR = 3;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ArduinoService arduinoService;
    private ESP32Service esp32Service;
    private ObjectMapper objectMapper;

    public MonitoringServiceImpl() {
    }


    public MonitoringServiceImpl(ArduinoService arduinoService, ESP32Service esp32Service, ObjectMapper objectMapper) {
        this.arduinoService = arduinoService;
        this.esp32Service = esp32Service;
        this.objectMapper = objectMapper;
    }

    @Override
    public void startMonitoring(String ip) {
        executorService.scheduleWithFixedDelay(() -> start(ip), 5, 60, TimeUnit.SECONDS);
        executorService.scheduleWithFixedDelay(this::check, 20, 60, TimeUnit.SECONDS);
    }

    @Override
    public void finishMonitoring() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    @SneakyThrows
    private void start(String ip) {
        for (int i = 0; i < DISTANCE_TO_SENSOR; i++) {
            arduinoService.sendData(Constants.FORWARD);
            Thread.sleep(1000);
        }
//        connectWiFi(SSID, SSID_PASS);

        BufferedWriter out = new BufferedWriter(new FileWriter(FILE_NAME, true));

        List<TimeSeriesDTO> data = esp32Service.getData(ip);
        System.out.println(data);

        for (TimeSeriesDTO item : data) {
            out.write("\n" + objectMapper.writeValueAsString(item));
        }
        out.close();


        for (int i = 0; i < DISTANCE_TO_SENSOR; i++) {
            arduinoService.sendData(Constants.BACK);
            Thread.sleep(1000);
        }
//        connectWiFi(SSID, SSID_PASS);

    }

    private void connectWiFi(String ssid, String password) throws IOException {

        Process process = Runtime.getRuntime().exec("nmcli device wifi connect " + ssid + " password " + password);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    @SneakyThrows
    private void check() {
        List<TimeSeriesDTO> data = Files.lines(Paths.get(FILE_NAME))
                .map(line -> {
                    try {
                        return objectMapper.readValue(line, TimeSeriesDTO.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Parsing error");
                    }
                })
                .collect(Collectors.toList());

        List<TimeSeriesDTO> dataToInspect = data.stream().skip(data.size() - 10).collect(Collectors.toList());
        if (dataToInspect.stream().mapToDouble(TimeSeriesDTO::getTemp).average().orElse(0) < 40
                || dataToInspect.stream().mapToDouble(TimeSeriesDTO::getHumid).average().orElse(0) < 30
                || dataToInspect.stream().mapToDouble(TimeSeriesDTO::getPressure).average().orElse(0) < 1000
        ) {
            System.out.println("Potential data warning for the last hour, check that everything is fine: " + FILE_NAME);
        }
    }
}
