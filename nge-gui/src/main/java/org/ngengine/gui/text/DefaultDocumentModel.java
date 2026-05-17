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

// import java.util.ArrayList;
// import java.util.List;
// import java.util.StringTokenizer;

// import org.ngengine.gui.core.VersionedReference;


// /**
//  *  An implementation of the DocumentModel 
//  *  that depends on caret and text passed by a composition
//  *  session
//  *
//  *  @author Riccardo Balbo
//  */
// public class DefaultDocumentModel implements DocumentModel, Cloneable {

//     private long version;
//     private String text = "";
//     private int[] caret = new int[2]; // from, to
//     public DefaultDocumentModel() {
//         setText("");
//     }

//     public DefaultDocumentModel( String text ) {
//         setText(text);
//     }
    
//     @Override
//     public DefaultDocumentModel clone() {
//         try {
//             DefaultDocumentModel result = (DefaultDocumentModel)super.clone();
            
//             result.text = this.text;
            
//             result.caret = new int[2];
//             result.caret[0] = this.caret[0];
//             result.caret[1] = this.caret[1];
            
//             // And reset the version because it's ok for this document to start
//             // over
//             result.version = 0;
 
//             return result;           
//         } catch( CloneNotSupportedException e ) {
//             throw new RuntimeException("Clone not supported", e);
//         }
//     }

//     @Override
//     public void setCaret(int from, int to) {
//         this.caret[0] = from;
//         this.caret[1] = to;
//     }
    

//     @Override
//     public void setText( String text ) {
//         if(text==null) text = "";
//         this.text = text;
//         version++;
//     }

//     @Override
//     public String getText() {
//         return this.text ;
//     }



//     @Override
//     public int[] getCaret() {
//         int from = caret[0];
//         int to = caret[1];
//         if( from > to ) {
//             int t = from;
//             from = to;
//             to = t;
//         }
//         if(from<0) from = 0;
//         if(to>text.length()) to = text.length();
//         caret[0] = from;
//         caret[1] = to;
//         return caret;
//     }


//     @Override
//     public long getVersion() {
//         return version;
//     }

//     @Override
//     public DocumentModel getObject() {
//         return this;
//     }

//     @Override
//     public VersionedReference<DocumentModel> createReference() {
//         return new VersionedReference<DocumentModel>(this);
//     }

//     @Override
//     public String toString() {
//         return getClass().getSimpleName() + "[]";
//     }

 
    
// }
