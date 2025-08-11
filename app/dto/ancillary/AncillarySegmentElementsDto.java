package dto.ancillary;

import java.util.List;

public class AncillarySegmentElementsDto {

    private List<Integer> segmentNumbers;

    private List<String> segmentTattooList;

    public List<Integer> getSegmentNumbers() {
        return segmentNumbers;
    }

    public void setSegmentNumbers(List<Integer> segmentNumbers) {
        this.segmentNumbers = segmentNumbers;
    }

    public List<String> getSegmentTattooList() {
        return segmentTattooList;
    }

    public void setSegmentTattooList(List<String> segmentTattooList) {
        this.segmentTattooList = segmentTattooList;
    }

}
