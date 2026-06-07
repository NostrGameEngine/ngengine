/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui.ime;

/**
 * An event with the current composition state.
 * @author Riccardo Balbo
 */
public class ImeCompositionEvent {
    private String text = "";
    private int cursor = 0;
    private int cursorEnd = 0;
    
    private boolean finished = false;
    private boolean multiline = false;

    private Runnable end = () -> {
        finished = true;
    };

    public ImeCompositionEvent(){
        this("", false);
    }

    public ImeCompositionEvent(String text){
        this(text, false);
    }

    public ImeCompositionEvent(String text, boolean multiline){
        this.text = text == null ? "" : text;
        this.multiline = multiline;
        this.cursor = this.text.length();
        this.cursorEnd = this.text.length();
    }

    public void end(){
        end.run();
    }

    public boolean isMultiline(){
        return multiline;
    }

    public String getText(){
        return text;
    }

    public int getCursorStart(){
        return cursor;
    }

    public int getCursorEnd(){
        return cursorEnd;
    }

    public boolean isFinished(){
        return finished;
    }   

    /**
     * Appends the given string at the current cursor position,
     * replacing any selected text.
     * @param str
     */
    public void insertAtCursor(String str){
        if (str == null) str = "";
        int from = Math.min(clamp(cursor), clamp(cursorEnd));
        int to = Math.max(clamp(cursor), clamp(cursorEnd));
        StringBuilder sb = new StringBuilder(text);
        sb.replace(from, to, str);
        text = sb.toString();
        cursor = from + str.length();
        cursorEnd = cursor;

    }

    public void setSelection(int from, int to){
        this.cursor = clamp(from);
        this.cursorEnd = clamp(to);
    }

    public void setCursor(int pos){
        this.cursor = clamp(pos);
        this.cursorEnd = this.cursor;
    }

    public void setText(String text){
        this.text = text == null ? "" : text;
        this.cursor = this.text.length();
        this.cursorEnd = this.text.length();
    }

    public void setMultiline(boolean multiline){
        this.multiline = multiline;
    }

    public void setEndAction(Runnable end){
        this.end = end;
    }

    public void setFinished(boolean finished){
        this.finished = finished;
    }

    private int clamp(int pos) {
        return Math.max(0, Math.min(pos, text.length()));
    }

}
