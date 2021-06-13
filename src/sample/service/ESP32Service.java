package sample.service;

import sample.model.TimeSeriesDTO;

import java.util.List;

public interface ESP32Service {
    boolean healthy(String ip);
    List<TimeSeriesDTO> getData(String ip);
}
