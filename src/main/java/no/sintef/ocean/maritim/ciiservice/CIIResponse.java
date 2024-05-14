package no.sintef.ocean.maritim.ciiservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CIIResponse {
    private int vesselCode;
    private List<ResponseDataPoint> dataPoints;

    public CIIResponse() {
        dataPoints = new ArrayList<ResponseDataPoint>();
    }
}
