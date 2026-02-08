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

package com.simsilica.lemur;


import java.util.logging.Logger;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiContext.GuiContextHandler;
import com.simsilica.lemur.nav.PopupHandler;
import com.simsilica.lemur.style.ElementId;


/**
 *  Provides modal option panel support where the option
 *  panel is the only thing that can receive mouse/touch input 
 *  until closed.
 *  
 *  Note: requires PopupState to have also been attached, which
 *  is done by GuiGlobals by default.  This class is now just a thin
 *  wrapper around the standard PopupState.
 *
 *  @author    Paul Speed
 */
public class OptionPanelState  implements GuiContextHandler{

    static Logger log = Logger.getLogger(OptionPanelState.class.getName());

    private OptionPanel current;
    private ElementId defaultElementId = new ElementId(OptionPanel.ELEMENT_ID);
    private final PopupHandler handler;
    private String style;


    public OptionPanelState(PopupHandler handler, String style ) {
        this.style = style;
        this.handler = handler;
    }

    public OptionPanelState( PopupHandler handler, ElementId defaultElementId, String style ) {
        this.defaultElementId = defaultElementId;
        this.style = style;
        this.handler = handler;
    }
    
    public OptionPanelState( PopupHandler handler) {
      
        this.handler = handler;
    }
    
    /** 
     *  Creates and displays a modal OptionPanel with the specified 
     *  settings.  The option panel will be visible until the user
     *  clicks a response or until close() is called.
     */
    public void show( String title, String message, Action... options ) {
        show(title, message, defaultElementId, options);
    }               
 
    /** 
     *  Creates and displays a modal OptionPanel with the specified 
     *  settings.  The option panel will be visible until the user
     *  clicks a response or until close() is called.
     */
    public void show( String title, String message, ElementId elementId, Action... options ) {
        show(new OptionPanel(title, message, elementId, style, options));
    }               
 
    protected String getName( Throwable t ) {
        String name = t.getClass().getSimpleName();
        StringBuilder sb = new StringBuilder();
        boolean last = true;
        sb.append(Character.toUpperCase(name.charAt(0)));
        for( int i = 1; i < name.length(); i++ ) {
            char c = name.charAt(i);
            boolean upper = Character.isUpperCase(c);
            if( upper && !last ) {
                sb.append(" ");
                last = true;
            }
            sb.append(c);
            last = upper;
        }
        return sb.toString();   
    }
 
    /**
     *  Creates and displays a model OptionPanel with the specified
     *  error information.  An attempt is made to construct a useful
     *  message for the specified Throwable.
     *  The option panel will be visible until the user
     *  clicks a response or until close() is called.
     */
    public void showError( String title, Throwable t ) {
        show(title, getName(t) + "\n" + t.getMessage(), defaultElementId);   
    }     
 
    /**
     *  Creates and displays a model OptionPanel with the specified
     *  error information.  An attempt is made to construct a useful
     *  message for the specified Throwable.
     *  The option panel will be visible until the user
     *  clicks a response or until close() is called.
     */
    public void showError( String title, Throwable t, ElementId elementId ) {
        show(title, getName(t) + "\n" + t.getMessage(), elementId);   
    }     
 
    /**
     *  Modally shows the specified OptionPanel in the guiNode as defined by
     *  getGuiNode().  An invisible blocker geometry is placed behind it
     *  to catch all mouse events until the panel is closed.  The option 
     *  panel will be visible until the user clicks a response or until 
     *  close() is called. 
     */
    public void show( OptionPanel panel ) {
        if( this.current != null ) {
            current.close();
        }
        
        this.current = panel;
 
        Vector2f screen = handler.getGuiSize();
        Vector3f pref = current.getPreferredSize();
        
        Vector3f pos = new Vector3f(screen.x, screen.y, 0).multLocal(0.5f);
        pos.x -= pref.x * 0.5f;
        pos.y += pref.y * 0.5f;
        current.setLocalTranslation(pos);
        
       handler.showModalPopup(current);
    }
 
    /**
     *  Closes an open OptionPanel if one is currently open.  Does
     *  nothing otherwise.
     */
    @Override
    public void close() {
        if( current != null ) {
            //current.close();
            handler.closePopup(current);
            current = null;
        }
    }     
 
    /**
     *  Returns the currently displayed OptionPanel or null if
     *  no option panel is visible.
     */
    public OptionPanel getCurrent() {
        return current;
    }
    
 
    @Override
    public void update( float tpf ) {
    }

    
}
