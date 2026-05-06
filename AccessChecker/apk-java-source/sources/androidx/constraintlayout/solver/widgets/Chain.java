package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;

/* loaded from: classes.dex */
class Chain {
    private static final boolean DEBUG = false;

    Chain() {
    }

    static void applyChainConstraints(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem system, int orientation) {
        int offset;
        int chainsSize;
        ChainHead[] chainsArray;
        if (orientation == 0) {
            offset = 0;
            chainsSize = constraintWidgetContainer.mHorizontalChainsSize;
            chainsArray = constraintWidgetContainer.mHorizontalChainsArray;
        } else {
            offset = 2;
            chainsSize = constraintWidgetContainer.mVerticalChainsSize;
            chainsArray = constraintWidgetContainer.mVerticalChainsArray;
        }
        for (int i = 0; i < chainsSize; i++) {
            ChainHead first = chainsArray[i];
            first.define();
            applyChainConstraints(constraintWidgetContainer, system, orientation, offset, first);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:210:0x067a  */
    /* JADX WARN: Removed duplicated region for block: B:213:0x0686  */
    /* JADX WARN: Removed duplicated region for block: B:215:0x068e  */
    /* JADX WARN: Removed duplicated region for block: B:220:0x06aa  */
    /* JADX WARN: Removed duplicated region for block: B:230:0x06b6  */
    /* JADX WARN: Removed duplicated region for block: B:232:0x06a6  */
    /* JADX WARN: Removed duplicated region for block: B:233:0x068b  */
    /* JADX WARN: Removed duplicated region for block: B:234:0x067f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void applyChainConstraints(androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer r45, androidx.constraintlayout.solver.LinearSystem r46, int r47, int r48, androidx.constraintlayout.solver.widgets.ChainHead r49) {
        /*
            Method dump skipped, instructions count: 1772
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.constraintlayout.solver.widgets.Chain.applyChainConstraints(androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer, androidx.constraintlayout.solver.LinearSystem, int, int, androidx.constraintlayout.solver.widgets.ChainHead):void");
    }
}
