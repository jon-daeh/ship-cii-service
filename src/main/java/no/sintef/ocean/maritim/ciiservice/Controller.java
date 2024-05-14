package no.sintef.ocean.maritim.ciiservice;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import no.sintef.ocean.maritim.libgymir.ciicalculator.CiiCalculator;
import no.sintef.ocean.maritim.libgymir.ciicalculator.CiiResult;
import no.sintef.ocean.maritim.libgymir.ciicalculator.CiiYearResult;
import no.sintef.ocean.maritim.libgymir.ciicalculator.FuelType;
import no.sintef.ocean.maritim.libgymir.ciicalculator.Vessel;
import no.sintef.ocean.maritim.libgymir.ciicalculator.VesselCategory;
import no.sintef.ocean.maritim.libgymir.logging.DataFramishTimeSeries;
import no.sintef.ocean.maritim.libgymir.logging.DataFramishTimeSeriesException;

@RestController
public class Controller implements API {
    private static final Map<String, String> vesselTypeMapping = new HashMap<>();
    private static final Map<String, String> fuelTypeMapping = new HashMap<>();

    static {
        vesselTypeMapping.put("Bulk carrier".toUpperCase(), "BULK");
        vesselTypeMapping.put("Gas carrier".toUpperCase(), "GAS");
        vesselTypeMapping.put("Tanker".toUpperCase(), "TANKER");
        vesselTypeMapping.put("Container ship".toUpperCase(), "CONTAINER");
        vesselTypeMapping.put("General cargo ship".toUpperCase(), "GENERAL");
        vesselTypeMapping.put("Refrigerated cargo carrier".toUpperCase(), "REFRIGERATED");
        vesselTypeMapping.put("Combination".toUpperCase(), "COMBINATION"); // Assuming a corresponding value
        vesselTypeMapping.put("LNG carrier".toUpperCase(), "LNG");
        vesselTypeMapping.put("Ro-ro cargo ship (vehicle carrier)".toUpperCase(), "RORO_VEHICLE");
        vesselTypeMapping.put("Ro-ro passenger ship".toUpperCase(), "RORO_PASSENGER");
        vesselTypeMapping.put("Ro-ro passenger ship designed to SOLAS chapter X".toUpperCase(),
                "RORO_PASSENGER_SOLASCHAPTERX");
        vesselTypeMapping.put("Cruise passenger ship".toUpperCase(), "CRUISE");

        fuelTypeMapping.put("HFO", FuelType.HFO.toString());
        fuelTypeMapping.put("MDO", FuelType.DIESEL.toString());
        fuelTypeMapping.put("IFO", FuelType.LFO.toString());
    }

    @Override
    public ResponseEntity<?> calculateCII(CIIRequest ciiRequest, Optional<String> horizon) {
        if (ciiRequest.getDataPoints().isEmpty()) {
            return ResponseEntity.unprocessableEntity().body("CII calculation not available without data points");
        }
        for (RequestDataPoint dataPoint : ciiRequest.getDataPoints()) {
            if (dataPoint.getCiiYear() < 2019) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available before 2019");
            }
            if (dataPoint.getCiiYear() > 2026) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available after 2026");
            }
            long hoursThisYear = java.time.Year.of(dataPoint.getCiiYear()).length() * 24L;
            if (dataPoint.getDurationHours() > hoursThisYear) {
                return ResponseEntity.unprocessableEntity()
                        .body("CII calculation not available for durations more than one year");
            }
            if (dataPoint.getDurationHours() < 0) {
                return ResponseEntity.unprocessableEntity()
                        .body("CII calculation not available for duration less than 0");
            }
            if (dataPoint.getSailedDistanceNm() <= 0) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available for distance <= 0");
            }
            if (dataPoint.getHfo() < 0) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available for hfo less than 0");
            }
            if (dataPoint.getIfo() < 0) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available for ifo less than 0");
            }
            if (dataPoint.getMdo() < 0) {
                return ResponseEntity.unprocessableEntity().body("CII calculation not available for mdo less than 0");
            }
            if (dataPoint.getMeCons() < 0) {
                return ResponseEntity.unprocessableEntity()
                        .body("CII calculation not available for me cons less than 0");
            }
        }
        if (!mapContainsString(vesselTypeMapping, ciiRequest.getVesselType())) {
            return ResponseEntity.unprocessableEntity()
                    .body("CII calculation not available for vessel type " + ciiRequest.getVesselType());
        }
        if (!mapContainsString(fuelTypeMapping, ciiRequest.getMeFuelType())) {
            return ResponseEntity.unprocessableEntity()
                    .body("CII calculation not available for fuel type " + ciiRequest.getMeFuelType());
        }
        if (ciiRequest.getDeadweightTonnage() <= 0) {
            return ResponseEntity.unprocessableEntity().body("CII calculation not available for deadweight <= 0");
        }
        if (ciiRequest.getGrossTonnage() <= 0) {
            return ResponseEntity.unprocessableEntity().body("CII calculation not available for gross tonnage <= 0");
        }
        String horizonValue = horizon.orElse("undefined");
        Vessel vessel = createVessel(ciiRequest);
        List<DataFramishTimeSeries> datas = null;
        try {
            datas = createTimeSeries(ciiRequest);
        } catch (DataFramishTimeSeriesException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        if (horizonValue == "short") {
            double totalDuration = 0.0;
            for (RequestDataPoint dataPoint : ciiRequest.getDataPoints()) {
                totalDuration += dataPoint.getDurationHours();
            }
            if (totalDuration > 3600 * 24 * 10) {
                return ResponseEntity.unprocessableEntity().body("CII calculation horizon too long");
            }
        }
        if (horizonValue == "medium") {
            double totalDuration = 0.0;
            for (RequestDataPoint dataPoint : ciiRequest.getDataPoints()) {
                totalDuration += dataPoint.getDurationHours();
            }
            if (totalDuration < 3600 * 24 * 10) {
                return ResponseEntity.unprocessableEntity().body("CII calculation horizon too short");
            }
        }
        CiiCalculator ciiCalculator = new CiiCalculator.CiiCalculatorBuilder()
                .withVessel(vessel)
                .withDistances("distance")
                .withConsumer("main_engine", getFuelType(ciiRequest))
                .withConsumer("hfo_consumption", FuelType.HFO)
                .withConsumer("ifo_consumption", FuelType.LFO)
                .withConsumer("mdo_consumption", FuelType.DIESEL)
                .build();

        CIIResponse response = new CIIResponse();
        response.setVesselCode(ciiRequest.getVesselCode());
        for (DataFramishTimeSeries data : datas) {
            CiiResult ciiResult = ciiCalculator.calculateCiiResult(data);
            if (ciiResult.ciiYearResults().size() > 1) {
                return ResponseEntity.unprocessableEntity().body("DataPoint duration more than one year");
            } else if (ciiResult.ciiYearResults().size() == 0) {
                return ResponseEntity.internalServerError().body("Error in CII calculation");
            }
            CiiYearResult ciiYearResult = null;
            for (Map.Entry<Integer, CiiYearResult> entry : ciiResult.ciiYearResults().entrySet()) {
                ciiYearResult = entry.getValue();
            }
            if (ciiYearResult == null) {
                return ResponseEntity.internalServerError().body("Error in CII calculation");
            }
            ResponseDataPoint dataPoint = new ResponseDataPoint();
            dataPoint.setScore(ciiYearResult.ciiScore());
            dataPoint.setRating(ciiYearResult.rating());
            dataPoint.setDurationHours((Duration.between(data.getFirstInstantInTimeSeries(), data.getTimeSeriesEnd())
                    .get(ChronoUnit.SECONDS) + 1) / 3600.0);
            dataPoint.setRequiredScore(ciiCalculator.getRequiredCiiForYear(ciiYearResult.year()));
            dataPoint.setRatingLimits(ciiCalculator.getRatingLimitsForYear(ciiYearResult.year()));
            dataPoint.setCiiYear(ciiYearResult.year());
            if (dataPoint.getScore() < dataPoint.getRequiredScore()) {
                dataPoint.setAlarm("healthy");
            } else if (dataPoint.getScore() > dataPoint.getRequiredScore()
                    && dataPoint.getRating().toLowerCase().equals("c")) {
                dataPoint.setAlarm("warning");
            } else if (dataPoint.getRating().toLowerCase().equals("d")) {
                dataPoint.setAlarm("caution");
            } else {
                dataPoint.setAlarm("alarm");
            }
            response.getDataPoints().add(dataPoint);

        }
        return ResponseEntity.ok(response);

    }

    private List<DataFramishTimeSeries> createTimeSeries(CIIRequest ciiRequest) throws DataFramishTimeSeriesException {
        List<DataFramishTimeSeries> datas = new ArrayList<>();
        for (RequestDataPoint dataPoint : ciiRequest.getDataPoints()) {
            DataFramishTimeSeries data = new DataFramishTimeSeries();
            int ciiYear = dataPoint.getCiiYear();
            LocalDate startOfYear = LocalDate.of(ciiYear, 1, 1);
            Instant janFirstInstant = startOfYear.atStartOfDay().toInstant(ZoneOffset.UTC);
            data.log(janFirstInstant.getEpochSecond(), "main_engine", dataPoint.getMeCons());
            data.log(janFirstInstant.getEpochSecond(), "hfo_consumption", dataPoint.getHfo());
            data.log(janFirstInstant.getEpochSecond(), "ifo_consumption", dataPoint.getIfo());
            data.log(janFirstInstant.getEpochSecond(), "mdo_consumption", dataPoint.getMdo());
            data.log(janFirstInstant.getEpochSecond(), "distance", dataPoint.getSailedDistanceNm());
            long duration = Math.round((dataPoint.getDurationHours() * 3600.0) - 1);
            data.setTimeSeriesEnd(janFirstInstant.plus(duration, ChronoUnit.SECONDS));
            datas.add(data);
        }
        return datas;
    }

    private Vessel createVessel(CIIRequest ciiRequest) {
        String vesselType = ciiRequest.getVesselType().toUpperCase();
        vesselType = vesselTypeMapping.getOrDefault(vesselType, vesselType);
        VesselCategory vesselCategory = VesselCategory.valueOf(vesselType);
        switch (vesselCategory.getCapacityType()) {
            case GT:
                return new Vessel(vesselCategory, ciiRequest.getGrossTonnage());
            case DWT:
                return new Vessel(vesselCategory, ciiRequest.getDeadweightTonnage());
            default:
                return null;
        }
    }

    private FuelType getFuelType(CIIRequest ciiRequest) {
        String fuelType = ciiRequest.getMeFuelType().toUpperCase();
        fuelType = fuelTypeMapping.getOrDefault(fuelType, fuelType);
        return FuelType.valueOf(fuelType);
    }

    private static boolean mapContainsString(Map<String, String> map, String target) {
        return map.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(target)) ||
                map.values().stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }

}
