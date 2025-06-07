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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son;

import com.jme3.asset.AssetManager;
import com.jme3.input.ChaseCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.AsyncAssetManager;
import org.ngengine.DevMode;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.ViewPortFragment;
import org.ngengine.demo.son.controls.BoatAnimationControl;
import org.ngengine.demo.son.controls.BoatControl;
import org.ngengine.demo.son.controls.NetworkControl;
import org.ngengine.demo.son.ocean.OceanAppState;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.gui.win.std.NHud;
import org.ngengine.network.P2PChannel;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener;
import org.ngengine.nostr4j.rtc.signal.NostrRTCAnnounce;
import org.ngengine.player.Player;
import org.ngengine.player.PlayerManagerComponent;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

public class PlayGameState
    implements
        Component<P2PChannel>,
        LogicFragment,
        ViewPortFragment,
        InputHandlerFragment,
        AssetLoadingFragment,
        ConnectionListener,
        MessageListener<HostedConnection>,
        NostrRTCRoomPeerDiscoveredListener {

    private static final Logger log = Logger.getLogger(PlayGameState.class.getName());

    private P2PChannel chan;
    private Map<HostedConnection, Spatial> remoteBoats = new HashMap<>();
    private volatile Spatial localBoat;
    private NHud hud;
    private ComponentManager componentManager;

    private NLabel hudSpeed;
    private Runner runner;
    private AssetManager assetManager;
    private ViewPort viewPort;
    private InputManager inputManager;

    @Override
    public Object getSlot() {
        return "mainState";
    }

    @Override
    public void loadAssets(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void receiveInputManager(InputManager inputManager) {
        this.inputManager = inputManager;
    }

    @Override
    public void receiveViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;
    }

    @Override
    public void onNudge(
        ComponentManager mng,
        Runner runner,
        DataStoreProvider dataStoreProvider,
        boolean firstTime,
        P2PChannel chan
    ) {
        mng.enableComponent(OceanAppState.class, chan);
        mng.enableComponent(PhysicsManager.class, chan);
    }

    public void reloadHud() {
        NWindowManagerComponent mng = componentManager.getComponent(NWindowManagerComponent.class);

        if (hud != null) {
            hud.close();
        }
        mng.showWindow(
            NHud.class,
            (win, err) -> {
                if (err != null) {
                    log.log(Level.SEVERE, "Error loading HUD", err);
                    return;
                }
                hud = win;
                hudSpeed = new NLabel("Speed: 0 km/h");
                hudSpeed.setTextVAlignment(VAlignment.Top);
                hudSpeed.setTextHAlignment(HAlignment.Right);
                hud.getTopRight().addChild(hudSpeed);
            }
        );
    }

    @Override
    public void onEnable(
        ComponentManager mng,
        Runner runner,
        DataStoreProvider dataStoreProvider,
        boolean firstTime,
        P2PChannel chan
    ) {
        try {
            this.runner = runner;
            this.componentManager = mng;
            this.chan = chan;
            DevMode.registerReloadCallback(
                this,
                () -> {
                    reloadHud();
                }
            );

            mng.getComponent(NWindowManagerComponent.class).closeAll();
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
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStoreProvider) {
        try {
            if (localBoat != null) {
                localBoat.removeFromParent();
            }
            chan.removeConnectionListener(this);
            chan.removeMessageListener(this);
            chan.removeDiscoveryListener(this);
            remoteBoats.clear();
            if (hud != null) {
                hud.close();
                hud = null;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error disabling GameAppState", e);
        }
    }

    @Override
    public void updateAppLogic(float tpf) {
        try {
            frame++;
            if (frame == 2) {
                spawnBoat(null);
                reloadHud();
            }

            if (localBoat != null) {
                BoatControl boatControl = localBoat.getControl(BoatControl.class);
                NetworkControl boatNetControl = localBoat.getControl(NetworkControl.class);
                if (boatNetControl == null) {
                    boatNetControl = new NetworkControl(assetManager);
                    localBoat.addControl(boatNetControl);
                }
                boatNetControl.sendUpdatePackets(remoteBoats.entrySet());

                float speed = boatControl.getLinearVelocity().length();
                hudSpeed.setText(String.format("Speed: %.2f km/h", speed * 3.6f));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error updating GameAppState", e);
        }
    }

    @Override
    public void onRoomPeerDiscovered(NostrPublicKey peerKey, NostrRTCAnnounce announce, NostrRTCRoomPeerDiscoveredState state) {
        log.info("Peer discovered: " + peerKey + " with announce: " + announce);
    }

    @Override
    public void messageReceived(HostedConnection source, Message m) {
        this.runner.run(() -> {
                try {
                    Spatial boat = remoteBoats.get(source);
                    if (boat == null) throw new IllegalStateException("Boat not found for source: " + source.getId());
                    NetworkControl boatNetControl = boat.getControl(NetworkControl.class);
                    if (boatNetControl == null) {
                        boatNetControl = new NetworkControl(assetManager);
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
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection conn) {
        log.info("Connection removed: " + conn.getId());
        Spatial boat = remoteBoats.remove(conn);
        if (boat != null) boat.removeFromParent();
    }

    public void spawnBoat(HostedConnection conn) {
        PlayerManagerComponent playerManager = componentManager.getComponent(PlayerManagerComponent.class);
        OceanAppState ocean = componentManager.getComponent(OceanAppState.class);
        PhysicsManager physics = componentManager.getComponent(PhysicsManager.class);

        boolean isRemote = conn != null;
        AsyncAssetManager assetManager = (AsyncAssetManager) this.assetManager;

        Node rootNode = getRootNode(viewPort);
        assetManager.runInLoaderThread(
            t -> {
                Node playerSpatial = (Node) assetManager.loadModel("Models/boat/boat.gltf");
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

                Consumer<Spatial> applyPlayerTexture = flag -> {
                    if (flag == null) return;
                    log.info("Found flag model in boat: " + flag.getName());
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
            },
            (Spatial playerSpatial, Throwable err) -> {
                if (err != null) {
                    log.log(Level.SEVERE, "Error loading boat model", err);
                    return;
                }
                log.info("Spawned " + (isRemote ? "remote" : "local") + " boat: " + playerSpatial.getName());

                physics.getPhysics().getPhysicsSpace().add(playerSpatial);
                ocean.add(playerSpatial);
                rootNode.attachChild(playerSpatial);
                if (!isRemote) {
                    Camera cam = viewPort.getCamera();
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
            }
        );
    }

    public void onJoyAxisEvent(JoyAxisEvent evt) {}

    public void onJoyButtonEvent(JoyButtonEvent evt) {}

    public void onMouseMotionEvent(MouseMotionEvent evt) {}

    public void onMouseButtonEvent(MouseButtonEvent evt) {}

    public void onKeyEvent(KeyInputEvent evt) {}

    @Override
    public void updateViewPort(ViewPort viewPort, float tpf) {}

    @Override
    public void onTouchEvent(TouchEvent evt) {}

    @Override
    public void loadViewPortFilterPostprocessor(AssetManager assetManager, FilterPostProcessor fpp) {}
}
