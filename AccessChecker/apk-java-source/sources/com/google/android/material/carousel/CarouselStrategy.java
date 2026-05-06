package com.google.android.material.carousel;

import android.view.View;

/* loaded from: classes.dex */
public abstract class CarouselStrategy {
    abstract KeylineState onFirstChildMeasuredWithMargins(Carousel carousel, View view);

    static float getChildMaskPercentage(float maskedSize, float unmaskedSize, float childMargins) {
        return 1.0f - ((maskedSize - childMargins) / (unmaskedSize - childMargins));
    }

    static int[] doubleCounts(int[] count) {
        int[] doubledCount = new int[count.length];
        for (int i = 0; i < doubledCount.length; i++) {
            doubledCount[i] = count[i] * 2;
        }
        return doubledCount;
    }

    boolean isContained() {
        return true;
    }

    boolean shouldRefreshKeylineState(Carousel carousel, int oldItemCount) {
        return false;
    }
}
