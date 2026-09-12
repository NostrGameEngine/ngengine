/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WebFramePacingTest {
    private static final long SECOND = 1_000_000_000L;
    private static final long MS = 1_000_000L;

    @Test
    void consecutiveFramesHaveTheSameStartToStartInterval() {
        long start = 0;
        for (int i = 0; i < 100; i++) {
            long end = start + 2 * MS;
            int delay = WebContext.frameDelayMillis(start, end, SECOND, 100);
            assertEquals(8, delay);
            long next = end + delay * MS;
            assertEquals(10 * MS, next - start);
            start = next;
        }
    }

    @Test
    void overrunsDoNotCreateCatchUpDebtInFollowingFrames() {
        assertEquals(0, WebContext.frameDelayMillis(0, 45 * MS, SECOND, 100));
        assertEquals(8, WebContext.frameDelayMillis(45 * MS, 47 * MS, SECOND, 100));
    }

    @Test
    void fractionalMillisecondsAreRoundedUpRatherThanStartingEarly() {
        assertEquals(15, WebContext.frameDelayMillis(0, 2 * MS, SECOND, 60));
        assertEquals(1, WebContext.frameDelayMillis(0, 16 * MS, SECOND, 60));
    }

    @Test
    void uncappedAndMissedFramesDoNotSleep() {
        assertEquals(0, WebContext.frameDelayMillis(0, MS, SECOND, 0));
        assertEquals(0, WebContext.frameDelayMillis(0, MS, SECOND, -1));
        assertEquals(0, WebContext.frameDelayMillis(0, 10 * MS, SECOND, 100));
    }

    @Test
    void changingTheCapUsesOnlyTheCurrentFrame() {
        assertEquals(8, WebContext.frameDelayMillis(0, 2 * MS, SECOND, 100));
        assertEquals(3, WebContext.frameDelayMillis(10 * MS, 12 * MS, SECOND, 200));
        assertEquals(18, WebContext.frameDelayMillis(15 * MS, 17 * MS, SECOND, 50));
    }
}
