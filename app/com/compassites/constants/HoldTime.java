package com.compassites.constants;

/**
 * Created by ritesh on 5/11/15.
 */
public enum HoldTime {
    ZERO_TO_ONE(0),
    TWO_TO_FIVE(4),
    SIX_TO_TEN(8),
    ELEVEN_TO_TWENTY(24),
    TWENTY_ONE_TO_THIRTY(72);

    private final int value;

    private HoldTime(int holdTime){
        this.value = holdTime;
    }

    public int getHoldTime(){
        return this.value;
    }
}
