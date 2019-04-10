package com.motions.music.facetracking;

import org.opencv.core.Rect;

public class NoseThresholder {
    NoseListener listener;

    public NoseThresholder() {
        listener = null;
    }
    public interface NoseListener {
        public void onNoseThresholdPassed(Rect r);
    }


    public void setNoseListener(NoseListener listener) {
        this.listener = listener;
    }
}
