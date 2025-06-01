package org.ngengine.demo.son;

 
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.AsyncAssetManager;
import org.ngengine.DevMode;
import org.ngengine.demo.son.controls.BoatAnimationControl;
import org.ngengine.demo.son.controls.BoatControl;
import org.ngengine.demo.son.controls.NetworkControl;
import org.ngengine.demo.son.ocean.OceanAppState;
import org.ngengine.demo.son.packets.AnimPacket;
import org.ngengine.demo.son.packets.TransformPacket;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.win.NWindowManagerAppState;
import org.ngengine.gui.win.std.NHud;
import org.ngengine.network.P2PChannel;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCAnnounce;
import org.ngengine.platform.AsyncTask;
import org.ngengine.player.Player;
import org.ngengine.player.PlayerManagerAppState;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;

public class GameAppState extends NGEAppState implements ConnectionListener, MessageListener<HostedConnection>,NostrRTCRoomPeerDiscoveredListener{
    private static final Logger log = Logger.getLogger(GameAppState.class.getName());
    private OceanAppState ocean;
    private BulletAppState physics;
    private P2PChannel chan;
    private Map<HostedConnection, Spatial> remoteBoats = new HashMap<>();
    private volatile Spatial localBoat;
    private NHud hud;

    private NLabel hudSpeed;
    public GameAppState(){
        setUnit("mainState");
        setEnabled(false);
        // this.chan = chan;
        
    }

    public void show(P2PChannel chan) {
        System.out.println("Showing GameAppState with channel: " + chan);
        this.chan = chan;
        setEnabled(true);
    }

    

   

    public void reloadHud(){
        NWindowManagerAppState mng = getStateManager().getState(NWindowManagerAppState.class);

        if(hud!=null){
            hud.close();
        } 
        mng.showWindow(NHud.class,(win,err)->{
            if(err!=null){
                log.log(Level.SEVERE, "Error loading HUD", err);
                return;
            }
            hud = win;
            hudSpeed  = new NLabel("Speed: 0 km/h");
            hudSpeed.setTextVAlignment(VAlignment.Top);
            hudSpeed.setTextHAlignment(HAlignment.Right);
            hud.getTopRight().addChild(hudSpeed);

        });
      
    
        
        
     }

    @Override
    protected void onEnable() {
        try{
            DevMode.registerReloadCallback(this,()->{
                reloadHud();
            });
            ocean = new OceanAppState();
            getStateManager().attach(ocean);
            physics = new BulletAppState();
            getStateManager().attach(physics);

            getStateManager().getState(NWindowManagerAppState.class).closeAll();

            // WindowManagerAppState wms = getState(WindowManagerAppState.class);
            // wms.showWindow(GameHud.class, null);
            chan.addConnectionListener(this);
            chan.addMessageListener(this);
            chan.addDiscoveryListener(this);        
        } catch (Exception e) {
            log.severe("Error initializing GameAppState: " + e.getMessage());
            e.printStackTrace();
        }
    }

    int frame = 0;
  

    @Override
    public void update(float tpf) {
        // if(localBoat!=null){
        //     localBoat.depthFirstTraversal(sx->{
        //         if (sx instanceof Geometry) {
        //             Material mat = ((Geometry) sx).getMaterial();
        //             mat.setVector3("HSVShift", new Vector3f(0.5f, -0.6f, -0.6f));
        //         }
        //     });
        // }
        try{
            frame++;
            if(frame==2){
                spawnBoat(null);
                reloadHud();
            }

            if(localBoat!=null){
                BoatControl boatControl = localBoat.getControl(BoatControl.class);
                NetworkControl boatNetControl = localBoat.getControl(NetworkControl.class);
                if(boatNetControl==null){
                    boatNetControl = new NetworkControl(getApplication().getAssetManager());
                    localBoat.addControl(boatNetControl);
                }
                boatNetControl.sendUpdatePackets(remoteBoats.entrySet());

                // lastTransformUpdate+=tpf;
                // if(lastTransformUpdate>1f/35f){
                //     lastTransformUpdate = 0f;
                //     Transform localBoatTransform = localBoat.getWorldTransform();
                //     TransformPacket packet = new TransformPacket(localBoatTransform);

                //     for (Map.Entry<HostedConnection,Spatial> b : remoteBoats.entrySet()) {
                //         try{
                //             Vector3f p = b.getValue().getWorldTranslation();
                //             boatNetControl.drawPacketSent(p.add(new Vector3f(0,3,0)));
                //             b.getKey().send(packet);
                //          }catch(Exception e){
                //             log.log(Level.WARNING,"Error sending transform packet to connection " + b.getKey().getId()  ,e);
                //         }
                //     }                             
     
                // }    
                

                // lastAnimUpdate+=tpf;
                // if(lastAnimUpdate>1f/15f){
                //     lastAnimUpdate=0;
                //     BoatAnimationControl animControl = localBoat.getControl(BoatAnimationControl.class); 
                //     if (animControl != null) {
                //         float flagFactor = animControl.getFlagFactor();
                //         float sailFactor = animControl.getSailFactor();
                //         float windFactor = animControl.getWindFactor();
                //         AnimPacket animPacket = new AnimPacket(flagFactor, sailFactor, windFactor);

                //         for (HostedConnection conn : remoteBoats.keySet()) {
                //             try {
                //                 conn.send(animPacket);
                //             } catch (Exception e) {
                //                 log.log(Level.WARNING,
                //                         "Error sending anim packet to connection " + conn.getId(), e);
                //             }
                //         }
                //     }

                   
                // }

                float speed = boatControl.getLinearVelocity().length();
                hudSpeed.setText(String.format("Speed: %.2f km/h", speed * 3.6f));


            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error updating GameAppState", e);
        }

    }

    @Override
    protected void onDisable() {
        getStateManager().detach(ocean);
        getStateManager().detach(physics);
        
    }

    @Override
    public void onRoomPeerDiscovered(NostrPublicKey peerKey, NostrRTCAnnounce announce,
            NostrRTCRoomPeerDiscoveredState state) {
                log.info("Peer discovered: " + peerKey + " with announce: " + announce);
      
    }

    @Override
    public void messageReceived(HostedConnection source, Message m) {
        getApplication().enqueue(()->{;
            try{
                Spatial boat = remoteBoats.get(source);
                if(boat==null)throw new IllegalStateException("Boat not found for source: " + source.getId());
                NetworkControl boatNetControl = boat.getControl(NetworkControl.class);
                if (boatNetControl == null) {
                    boatNetControl = new NetworkControl(getApplication().getAssetManager());
                    boat.addControl(boatNetControl);
                }
                boatNetControl.applyPacket(m);                         
            } catch (Exception e) {
                log.log(Level.WARNING, "Error processing message from connection " + source.getId(), e);
            }
        });
       
    }

    @Override
    public void connectionAdded(Server server, HostedConnection conn) {
        log.info("New connection: " + conn.getId());
        spawnBoat(conn);
        // .then(boat -> {
        //     
        //     boat.setName("RemoteBoat_" + conn.getAddress());            
        //     log.info("Spawned remote boat for connection: " + conn.getId());
        //     return null;
        // }).catchException(ex -> {
        //     log.log(Level.SEVERE,"Error spawning remote boat",ex);
        // });
        
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection conn) {
        log.info("Connection removed: " + conn.getId());
        remoteBoats.remove(conn);
      
    }
    
    public void spawnBoat(HostedConnection conn) {
        boolean isRemote = conn != null;
        AppStateManager stateManager = getStateManager();
        AsyncAssetManager assetManager = AsyncAssetManager.of(this.getApplication().getAssetManager(), getApplication());

        Node rootNode = (Node)getApplication().getViewPort().getScenes().get(0);
        assetManager.runInLoaderThread(t -> {
            Node playerSpatial = (Node)assetManager.loadModel("Models/boat/boat.gltf");
            playerSpatial.addControl(new BoatAnimationControl());

            playerSpatial.depthFirstTraversal(sx -> {
                if (sx instanceof Geometry) {
                    Material mat = ((Geometry) sx).getMaterial();
                    // Material newMat = mat.clone();
                    Material newMat = new Material(assetManager, "Materials/PBR.j3md");
                    for (MatParam matParam : mat.getParams()) {
                        newMat.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
                    }
                    newMat.getAdditionalRenderState().set(mat.getAdditionalRenderState());
                    sx.setMaterial(newMat);
                    DevMode.registerForReload(newMat);

                }
            });
            playerSpatial.setShadowMode(ShadowMode.CastAndReceive);

            Consumer<Spatial> applyPlayerTexture = (flag) -> {
                if(flag==null)return;
                log.info("Found flag model in boat: " + flag.getName());
                PlayerManagerAppState playerManager = stateManager.getState(PlayerManagerAppState.class);
                Player player = null;
                if (conn != null) {
                    player = playerManager.getPlayer(conn);
                } else {
                    player = playerManager.getPlayer(chan);
                }

                Texture2D image = player.getImage();
                if (image != null) {
                    flag.depthFirstTraversal(sx -> {
                        if (sx instanceof Geometry) {
                            Material mat = ((Geometry) sx).getMaterial();
                            mat.setTexture("BaseColorMap", image);

                        }
                    });
                }
            };

            Spatial sail = playerSpatial.getChild("sail");
            applyPlayerTexture.accept(sail);

            Spatial sail2 = playerSpatial.getChild("sail2");
            applyPlayerTexture.accept(sail2);

            Spatial flag = playerSpatial.getChild("flag");
            applyPlayerTexture.accept(flag);


            // BiConsumer<Spatial, Vector3f> applyPlayerColor = (boat, hsv) -> {
            // boat.depthFirstTraversal(sx -> {
            // if (sx instanceof Geometry) {
            // Material mat = ((Geometry) sx).getMaterial();
            // mat.setVector3("HSVShift",hsv);
                        
            // }
            // });
            // };

            Spatial boat = playerSpatial.getChild("boat");
            // applyPlayerColor.accept(boat, new Vector3f(0,0,0));

            BoatControl playerPhysics = new BoatControl(isRemote, 100f);

            // RigidBodyControl playerPhysics = new RigidBodyControl(100f);
            playerSpatial.addControl(playerPhysics);
            Vector3f pos = new Vector3f(0, 0, 0);
            pos.y = ocean.getWaterHeightAt(pos.x, pos.z);
            playerPhysics.setPhysicsLocation(pos);
            return playerSpatial;
        }, (Spatial playerSpatial, Throwable err)->{
            if(err!=null){
                log.log(Level.SEVERE, "Error loading boat model", err);
                return;
            }
            log.info("Spawned "+(isRemote ? "remote" : "local") + " boat: " + playerSpatial.getName());

            stateManager.getState(BulletAppState.class).getPhysicsSpace().add(playerSpatial);
            stateManager.getState(OceanAppState.class).add(playerSpatial);
            rootNode.attachChild(playerSpatial);
            if (!isRemote) {
                InputManager inputManager = getApplication().getInputManager();
                Camera cam = getApplication().getCamera();
                inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
                inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
                inputManager.addMapping("SteerLeft", new KeyTrigger(KeyInput.KEY_A));
                inputManager.addMapping("SteerRight", new KeyTrigger(KeyInput.KEY_D));

                BoatControl playerPhysics = playerSpatial.getControl(BoatControl.class);
                inputManager.addListener(playerPhysics, "Forward", "Backward", "SteerLeft", "SteerRight");

                ChaseCamera chaseCam = new ChaseCamera(cam, playerSpatial, inputManager);
                chaseCam.setSmoothMotion(false);
                chaseCam.setDefaultDistance(200f);
                chaseCam.setMinDistance(100f);
                chaseCam.setMaxDistance(400f);
                chaseCam.setTrailingEnabled(false);
                chaseCam.setMinVerticalRotation(0.2f);

                chaseCam.setDragToRotate(false);

                chaseCam.setUpVector(Vector3f.UNIT_Y);
                chaseCam.setSpatial(playerSpatial);
                localBoat = playerSpatial;
            } else {
                remoteBoats.put(conn, playerSpatial);
            }
         });
      
      
    }
}
