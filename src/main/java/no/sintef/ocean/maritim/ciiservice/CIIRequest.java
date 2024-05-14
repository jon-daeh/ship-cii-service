package no.sintef.ocean.maritim.ciiservice;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CIIRequest {
    private int vesselCode;
    private String vesselType;
    private int grossTonnage;
    private int deadweightTonnage;
    private String meFuelType;
    private List<RequestDataPoint> dataPoints;
}
