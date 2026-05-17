// /*
//  * $Id$
//  * 
//  * Copyright (c) 2016, Simsilica, LLC
//  * All rights reserved.
//  * 
//  * Redistribution and use in source and binary forms, with or without
//  * modification, are permitted provided that the following conditions 
//  * are met:
//  * 
//  * 1. Redistributions of source code must retain the above copyright 
//  *    notice, this list of conditions and the following disclaimer.
//  * 
//  * 2. Redistributions in binary form must reproduce the above copyright 
//  *    notice, this list of conditions and the following disclaimer in 
//  *    the documentation and/or other materials provided with the 
//  *    distribution.
//  * 
//  * 3. Neither the name of the copyright holder nor the names of its 
//  *    contributors may be used to endorse or promote products derived 
//  *    from this software without specific prior written permission.
//  * 
//  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
//  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
//  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
//  * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
//  * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
//  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
//  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
//  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
//  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
//  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
//  * OF THE POSSIBILITY OF SUCH DAMAGE.
//  */

// package org.ngengine.gui.text;

// import org.ngengine.gui.core.VersionedObject;
// import org.ngengine.gui.core.VersionedReference;

// /**
//  *  DocumentModel is a container for text that provides basic editing
//  *  interaction as used by things like TextField.
//  *
//  *  @author    Paul Speed
//  */
// public interface DocumentModel extends VersionedObject<DocumentModel> {

//     /**
//      *  Deep clones this document model. 
//      */
//     public DocumentModel clone();

//     /**
//      *  Replaces the text contained in this DocumentModel.
//      */
//     public void setText( String text );

//     /**
//      *  Returns the current text value contained in this DocumentModel.
//      */
//     public String getText(); 


//     public void setCaret( int from, int to );

//     public default void setCaret( int pos ) {
//         setCaret(pos, pos);
//     }

//     /**
//      *  Returns the current 'caret' position.  The 'caret' is where
//      *  new text characters will be inserted.  It's the current edit 
//      *  position.
//      */
//     public int[] getCaret(); 


 

  
   


// } 
