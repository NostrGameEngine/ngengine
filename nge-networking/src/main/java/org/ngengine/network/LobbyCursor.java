/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.network;

import java.time.Instant;
import java.util.List;

public class LobbyCursor {
    public enum Direction {
        NEWER,
        OLDER
    }

    private final Instant until;
    private final List<Lobby> lobbies;
    private final Instant since;
    private Direction direction = Direction.OLDER;

    public LobbyCursor(Direction direction, Instant until, Instant since, List<Lobby> lobbies) {
        this.until = until;
        this.lobbies = lobbies;
        this.since = since;
    }

    public Instant until() {
        return until;
    }

    public Instant since() {
        return since;
    }

    public List<Lobby> get() {
        return lobbies;
    }    

    public boolean hasMore() {
        return lobbies.size() > 0;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }

    @Override
    public String toString() {
        return "LobbyCursor{" + "direction=" + direction + ", until=" + until + ", since=" + since + '}';
    }   



    
}
