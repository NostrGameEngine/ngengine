package org.ngengine.gui.components;

 
import java.util.function.Consumer;

import org.ngengine.gui.qr.QrCode;
import org.ngengine.platform.NGEPlatform;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.noise.Color;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.core.GuiComponent;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.event.MouseListener;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleAttribute;
import org.ngengine.platform.NGEPlatform;
public class NQrViewer extends Container implements GuiControlListener, GuiUpdateListener, MouseListener{
    public static final String ELEMENT_ID =  "qr";
    public enum ErrorCorrectionLevel {
        LOW,
        MEDIUM,
        QUARTILE,
        HIGH;
    }
    protected String value;
    protected boolean secret = true;
    protected boolean show = false;
    protected ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.MEDIUM;
    protected final QuadBackgroundComponent background = new NAspectPreservingQuadBackground();
    protected ColorRGBA lightColor;
    protected ColorRGBA darkPixelsColor ;

    protected Label label = new Label("",new ElementId(ELEMENT_ID).child("label"));
    protected boolean invalidated = false;

    protected Container iconBar;
    protected Container qrCode;
    protected int qrSize = 0;
    protected int qrPreferredSize = 100;

    protected Consumer<String> copyAction;
    protected Consumer<String> clickAction = (src)->{
        if(copyAction != null){
            copyAction.accept(value);
        }
    };

    public NQrViewer() {
        this("");
    }

    public NQrViewer(String text) {
        super(new SpringGridLayout(Axis.Y,Axis.X,FillMode.None,FillMode.None),new ElementId( ELEMENT_ID));
        this.value = text;


        Container qrContainer  = new Container(new SpringGridLayout(Axis.Y,Axis.X,FillMode.None,FillMode.None), new ElementId(ELEMENT_ID).child("qrContainer"));
        setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        this.iconBar = new Container(new BoxLayout(Axis.X, FillMode.None), new ElementId("qr").child("iconContainer"));
        this.iconBar.setInsetsComponent(new DynamicInsetsComponent(0f, 1f, 0f, 0f));

        this.qrCode = new Container(new BorderLayout(), new ElementId("qr").child("qrCode"));
        this.qrCode.setBackground(background);
        
        qrContainer.addChild(qrCode);
        qrContainer.addChild(iconBar);
        qrContainer.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));

        addChild(label);
        addChild(qrContainer );

        qrCode.addMouseListener(this);
        label.setTextHAlignment(HAlignment.Center);
        label.setTextVAlignment(VAlignment.Center);

        setCopyAction(src->{
            NGEPlatform platform = NGEPlatform.get();
            platform.setClipboardContent(src);
        });
        
        getControl(GuiControl.class).addListener(this);
        getControl(GuiControl.class).addUpdateListener(this);
        invalidate();
    }

    public void setQrSize(int qrSize) {
        this.qrSize = qrSize;
        if(qrSize>qrPreferredSize){
            qrPreferredSize = qrSize;
        }
        invalidate();
    }

    public int getQrSize() {
        return qrSize;
    }

    public void setQrPreferredSize(int qrPreferredSize) {
        this.qrPreferredSize = qrPreferredSize;
        invalidate();
    }

    public int getQrPreferredSize() {
        return qrPreferredSize;
    }

    public void setLabel(String text) {
        label.setText(text == null ? "" : text);
    }

    public void setLabelHAlignment(HAlignment alignment) {
        label.setTextHAlignment(alignment);
    }

    public void setLabelVAlignment(VAlignment alignment) {
        label.setTextVAlignment(alignment);
    }

    public void setCopyAction(Consumer<String> action) {
        this.copyAction = action;
    }

    public void setClickAction(Consumer<String> action) {
        this.clickAction = action;
    }   

    @StyleAttribute(value="lightPixelsColor", lookupDefault=false)
    public void setLightPixelsColor(ColorRGBA color) {
        this.lightColor=color.clone();
        invalidate();
    }

    @StyleAttribute(value="darkPixelsColor", lookupDefault=false)
    public void setDarkPixelsColor(ColorRGBA color) {
        this.darkPixelsColor=color;
        invalidate();
    }




    public void setErrorCorrectionLevel(ErrorCorrectionLevel errorCorrectionLevel) {
        this.errorCorrectionLevel = errorCorrectionLevel;
        invalidate();
    }   

    public boolean isSecret(){
        return secret;
    }

    public void setIsSecret(boolean secret) {
        this.secret = secret;
        invalidate();        
    }

    public void setValue(String value) {
        this.value=value;
        invalidate();
    }

    public String getValue() {
        return value;
    }

    protected void invalidate() {
        invalidated = true;
    }

    protected void repaint(){
   
    
 
        if(qrPreferredSize<=0){
            qrCode.setPreferredSize(null);
        }else{
            qrCode.setPreferredSize(new Vector3f(qrPreferredSize, qrPreferredSize, 0));
        }

        if(qrSize>0){
            qrCode.setSize(new Vector3f(qrSize, qrSize, 0));
        }
    
     
        if(!isShown()){
            Texture texture = GuiGlobals.getInstance().loadTexture("ui/blurred-qr.png", false, false);
            background.setTexture(texture);
            background.setColor(lightColor);
          

        }else{

            QrCode.Ecc ecc = QrCode.Ecc.MEDIUM;
            switch (errorCorrectionLevel) {
                case LOW:
                    ecc = QrCode.Ecc.LOW;
                    break;
                case MEDIUM:
                    ecc = QrCode.Ecc.MEDIUM;
                    break;
                case QUARTILE:
                    ecc = QrCode.Ecc.QUARTILE;
                    break;
                case HIGH:
                    ecc = QrCode.Ecc.HIGH;
                    break;
            }
            QrCode qr = QrCode.encodeText(value, ecc);
          

            
            int margin = (int)(qrSize*0.07f);
            Image img = toImage(qr, qrSize-margin, margin);
            Texture2D texture = new Texture2D(img);
            background.setTexture(texture);   
           
        }
        
        
        this.qrCode.clearChildren();
        this.iconBar.clearChildren();
        
        if(isSecret()){
             
            NIconButton showBtn = new NIconButton( "icons/outline/eye.svg" );
            NIconButton hideBtn = new NIconButton("icons/outline/eye-off.svg");

            showBtn.addClickCommands((src) -> {
                if(!show){
                    show=true;
                    invalidate();
                }
            });

            hideBtn.addClickCommands((src) -> {
                if(show){
                    show=false;
                    invalidate();
                }
            });
            

                
            showBtn.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));
            this.iconBar.addChild(hideBtn);

            if(!isShown()){
                hideBtn.setAlpha(0);
                showBtn.setAlpha(1);
                this.qrCode.addChild(showBtn, BorderLayout.Position.Center, BorderLayout.Position.Center);
            } else{
                showBtn.setAlpha(0);
                hideBtn.setAlpha(1);
                showBtn.removeFromParent();
            }

        }

        if(copyAction!=null){
            NIconButton copyBtn = new NIconButton( "icons/outline/copy.svg" );
            copyBtn.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));
            copyBtn.addClickCommands((src) -> {
                clickAction.accept(value);
            });
            this.iconBar.addChild(copyBtn);
        }
        


    }


    public boolean isShown(){
        return !secret || show;
    }

    protected Image toImage(QrCode qr, int qrSize, int margin ) {
        int size =  qrSize + margin * 2;
        Image img = new Image(Format.RGBA8,size, 
                size, BufferUtils.createByteBuffer(
                        size* size*Format.RGBA8.getBitsPerPixel()/8),null,ColorSpace.Linear);
        ImageRaster imgr = ImageRaster.create(img);
        
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if(x < margin || y < margin || x >= size - margin || y >= size - margin) {
                    imgr.setPixel(x, y, darkPixelsColor);
                    continue;
                }

                int xQr = (int) ((float)(x - margin) / (float)qrSize * qr.size);
                int yQr = (int) ((float)(y - margin) / (float)qrSize * qr.size);
                boolean color = qr.getModule(xQr, yQr );
                imgr.setPixel(x, y, color ? lightColor : darkPixelsColor);
            }
        }
        return img;
    }

    @Override
    public void reshape(GuiControl source, Vector3f pos, Vector3f size) {
        //  invalidate();
    }

    @Override
    public void focusGained(GuiControl source) {
      
    }

    @Override
    public void focusLost(GuiControl source) {
       
    }

    @Override
    public void guiUpdate(GuiControl source, float tpf) {
        if(invalidated){
            repaint();
            invalidated = false;
        }
    }

    @Override
    public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
        if(event.isPressed()&&event.getButtonIndex() == 0) {
            if(this.isShown()){
                clickAction.accept(value);
            }
        }
    }

    @Override
    public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
        
    }

    @Override
    public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
       
    }

    @Override
    public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
      
    }
    
}
