/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import java.time.Instant;

/**
 * Central engine clock delegating to a replaceable {@link Provider}.
 */
public final class Clock {

    public interface Provider {
        long nanoTime();
        long currentTimeMillis();
        Instant now();
    }

    private static final Provider SYSTEM_PROVIDER = new Provider() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public Instant now() {
            return Instant.now();
        }
    };

    private static volatile Provider provider = SYSTEM_PROVIDER;

    private Clock() {
    }

    public static long nanoTime() {
        return provider.nanoTime();
    }

    public static long currentTimeMillis() {
        return provider.currentTimeMillis();
    }

    public static Instant now() {
        return provider.now();
    }

    public static Provider getProvider() {
        return provider;
    }

    public static void setProvider(Provider provider) {
        Clock.provider = provider != null ? provider : SYSTEM_PROVIDER;
    }

    public static void resetProvider() {
        Clock.provider = SYSTEM_PROVIDER;
    }
}
