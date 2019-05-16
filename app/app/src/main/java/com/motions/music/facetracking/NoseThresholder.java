package com.motions.music.facetracking;

import org.opencv.core.Rect;

/**
 * Class used for nose listening, threholder class really doesn't do anything
 */
public class NoseThresholder {
    NoseListener listener;

    public NoseThresholder() {
        listener = null;
    }

    /**
     * Interface is used in case multiple uses necessary
     */
    public interface NoseListener {
        void onNoseThresholdPassed(Rect r);
    }

    /**
     * Assign the custom implementation
     * @param listener custom listener
     */
    public void setNoseListener(NoseListener listener) {
        this.listener = listener;
    }
}
