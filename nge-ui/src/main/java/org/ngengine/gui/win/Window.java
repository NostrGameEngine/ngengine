package org.ngengine.gui.win;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.gui.components.IconButton;
import org.ngengine.gui.win.std.ErrorWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiLayout;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.ElementId;

public abstract class Window<T> extends Container implements GuiUpdateListener, GuiControlListener {
    private static final Logger log = Logger.getLogger(Window.class.getName());
    public static final String ELEMENT_ID  = "window";
    private  Container titleBar;
     private IconButton backButton;
    private IconButton placeHolderButton;
    private  Label title ;
    private  WindowManagerAppState appState;
    private  Container windowContent;

    private boolean center = true;
 
    private boolean fitContent = true;
    private T args;

    private final List<WindowListener> closeListeners = new CopyOnWriteArrayList<>();

    protected Window(){
        super(new BorderLayout(), new ElementId(ELEMENT_ID));
    }

    protected Window(GuiLayout layout, ElementId id){
        super(layout, id);
    }

    final void initialize(WindowManagerAppState appState,  Consumer<Window<T>> backAction) {
        this.appState = appState;
        titleBar = new Container(new BorderLayout(),
                new ElementId("window.titleBar"));
         
        backButton = new IconButton("icons/outline/chevron-left.svg");
        backButton.addClickCommands((src)->{
            if(backAction!=null){
                backAction.accept(this);
            }
        });
        placeHolderButton = new IconButton("icons/outline/chevron-left.svg");
        placeHolderButton.setCullHint(CullHint.Always);

        title = new Label("", new ElementId( "window.title"));
        title.setTextHAlignment(HAlignment.Center);
        title.setTextVAlignment(VAlignment.Center);
        float margin = title.getFontSize();
        title.setInsets(new Insets3f(0,margin,0,margin));
        titleBar.addChild(title, BorderLayout.Position.Center);
        
        addChild(titleBar, BorderLayout.Position.North);

        windowContent = new Container(new BoxLayout(Axis.Y, FillMode.None),new ElementId("window.content"));
        // windowContent.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));
        addChild(windowContent , BorderLayout.Position.Center);
        setBackAction(backAction);

        getControl(GuiControl.class).addListener(this);
        getControl(GuiControl.class).addUpdateListener(this);
        invalidate();
    }

    final void setArgs(T args){
        this.args = args;
    }

    public final  void setFitContent(boolean fitContent) {
        this.fitContent = fitContent;
        invalidate();
    }

    public final void addWindowListener(WindowListener listener) {
        if (listener != null && !closeListeners.contains(listener)) {
            closeListeners.add(listener);
        }
    }   

    public final  void removeWindowListener(WindowListener listener) {
        if (listener != null) {
            closeListeners.remove(listener);
        }
    }

    protected abstract void compose(Vector3f size, T args) throws Throwable;

    protected final void onShow(){
        for (WindowListener listener : closeListeners) {
            listener.onShow(this);
        }

    }

    protected final void onHide(){
        for (WindowListener listener : closeListeners) {
            listener.onHide(this);
        }

    }

    public final  void setCenter(boolean center) {
        this.center = center;
        invalidate();

    }

    protected final  WindowManagerAppState getManager(){
        return appState;
    }

    protected Container getContent() {
        return windowContent;
    }

    protected final  Container getContent(GuiLayout layout) {
        windowContent.setLayout(layout);
        return windowContent;
    }
    
    public final  void setTitle(String title) {
        this.title.setText(title);
    }

    public final  void setBackAction(Consumer<Window<T>> backAction) {
        if(backAction==null&& backButton!=null){
            backButton.removeFromParent();
            placeHolderButton.removeFromParent();
        }else{
            titleBar.addChild(backButton, BorderLayout.Position.West);
            titleBar.addChild(placeHolderButton, BorderLayout.Position.East);
        }
     }
 
     final  void recenter(Vector3f size) {
        getManager().runInThread(()->{
            int width = getManager().getWidth();
            int height = getManager().getHeight();

            setLocalTranslation(width / 2 - size.x / 2, height / 2 + size.y / 2, 1);
        });
    
    }

    @Override
    public final void reshape(GuiControl source, Vector3f pos, Vector3f size) {        
        if (center) {
            recenter(size);
        }
    }

    @Override
    public final void focusGained(GuiControl source) {
        
    }

    @Override
    public final  void focusLost(GuiControl source) {
        
    }

    protected int initStage = 0;

    protected final  void invalidate(){
        initStage=0;
    }

    protected final  boolean reloadNow(){
        Vector3f size = getSize().clone();
        if (size.length() == 0) return false;

        getContent().clearChildren();

        try {
            compose(size, args);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Failed to compose window content", e);
            getManager().closeWindow(this);
            getManager().showWindow(ErrorWindow.class, e);
            return true;
        }

        if (!fitContent) {
            int w = getManager().getWidth() / 2;
            int h = getManager().getHeight() / 2;
            if (w < 800) w = 800;
            if (h < 600) h = 600;
            if (w > getManager().getWidth()) w = getManager().getWidth();
            if (h > getManager().getHeight()) h = getManager().getHeight();

            setPreferredSize(new Vector3f(w, h, 0));
        } else {
            setPreferredSize(null);
        }
        return true;
        
    }

    @Override
    public final  void guiUpdate(GuiControl source, float tpf) {
        if(initStage==0){
            if(reloadNow()) initStage=1;
        }else  if (initStage == 1) {
            if (reloadNow()) initStage = 2;
        }
        if (center) {
            recenter(getSize());
        }
        
    }

    public final  void close(){
        getManager().runInThread(()->{
            getManager().closeWindow(this);
        });
    }

 
   
    
}
