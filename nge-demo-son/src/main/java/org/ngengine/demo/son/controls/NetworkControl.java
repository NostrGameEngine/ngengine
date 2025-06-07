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
package org.ngengine.demo.son.controls;

import com.jme3.asset.AssetManager;
import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.influencers.EmptyParticleInfluencer;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.demo.son.packets.AnimPacket;
import org.ngengine.demo.son.packets.TransformPacket;
import org.ngengine.network.RemotePeer;

public class NetworkControl extends AbstractControl {

    private static final Logger log = Logger.getLogger(NetworkControl.class.getName());

    private ParticleEmitter dataParticle;
    private Material dataParticleMaterial;
    private List<Particle> emittedParticle = new ArrayList<>(1);
    private final double NETSYNC_MIN_RATE = 1000.0 / 5.0;
    private final double NETSYNC_MAX_RATE = 1000.0 / 25.0;
    private final double MAX_D = 2000.0;
    private Spatial dataStreamSpatial;
    private Instant lastReceivedAnimPacket = Instant.ofEpochMilli(0);
    private Instant lastReceivedTransformPacket = Instant.ofEpochMilli(0);

    public NetworkControl(AssetManager assetManager) {
        dataParticleMaterial = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        dataParticle = new ParticleEmitter("Data Particles", Type.Triangle, 101);
        dataParticle.setParticleInfluencer(new EmptyParticleInfluencer());
        dataParticle.setInWorldSpace(true);
        dataParticle.setMaterial(dataParticleMaterial);
        dataParticle.setParticlesPerSec(0);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial != null && spatial instanceof Node) {
            Node node = (Node) spatial;
            node.attachChild(dataParticle);
        }
        dataStreamSpatial = null;
    }

    protected void drawPacketSent(Vector3f to) {
        dataParticle.setStartColor(ColorRGBA.Magenta.clone().setAlpha(0.8f));
        dataParticle.setEndColor(ColorRGBA.Pink.clone().setAlpha(0f));
        dataParticle.setStartSize(0.3f);
        dataParticle.setEndSize(0.3f);
        dataParticle.setLowLife(2.5f);
        dataParticle.setHighLife(2.5f);

        emittedParticle.clear();
        dataParticle.emitParticles(1, emittedParticle);
        Particle particle = emittedParticle.size() > 0 ? emittedParticle.get(0) : null;
        if (particle == null) return;
        if (dataStreamSpatial == null) {
            spatial.depthFirstTraversal(sx -> {
                if (sx.getUserData("datastreamemitter") != null) {
                    dataStreamSpatial = sx;
                }
            });
            if (dataStreamSpatial == null) {
                dataStreamSpatial = spatial;
            }
        }
        particle.position.set(dataStreamSpatial.getWorldTranslation());
        particle.angle = 0f;
        particle.rotateSpeed = 0f;
        particle.velocity.set(to.subtract(particle.position).normalizeLocal().mult(120.3f));
    }

    public void sendUpdatePackets(Set<Map.Entry<HostedConnection, Spatial>> peers) {
        Transform localTransform = getSpatial().getWorldTransform();
        TransformPacket positionPacket = null;
        AnimPacket animPacket = null;

        long now = Instant.now().toEpochMilli();

        double baseMinRate = NETSYNC_MIN_RATE;
        double baseMmaxRate = NETSYNC_MAX_RATE;
        double maxDistance = MAX_D;

        for (Map.Entry<HostedConnection, Spatial> p : peers) {
            try {
                double maxRate = baseMmaxRate;
                double minRate = baseMinRate;

                if (p.getKey() instanceof RemotePeer) {
                    RemotePeer peer = (RemotePeer) p.getKey();
                    if (peer.getSocket().isUsingTURN()) {
                        minRate = 1000.0;
                        maxRate = 1000.0 / 10.0;
                    }
                }

                Vector3f pPos = p.getValue().getWorldTranslation();
                float dist = pPos.distance(localTransform.getTranslation());
                double scale = Math.clamp((double) (dist / maxDistance), 0.0, 1.0);
                double expectedRate = ((1.0 - scale) * maxRate) + (scale * minRate);

                Long lastSentPosition = p.getKey().getAttribute("lspp");
                if (lastSentPosition == null) lastSentPosition = 0l;

                if (now - lastSentPosition >= expectedRate) {
                    p.getKey().setAttribute("lspp", now);
                    if (positionPacket == null) {
                        positionPacket = new TransformPacket();
                        positionPacket.setTransform(localTransform);
                    }

                    try {
                        p.getKey().send(positionPacket);
                        drawPacketSent(pPos);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error sending transform packet to connection " + p.getKey().getId(), e);
                    }

                    if (animPacket == null) {
                        BoatAnimationControl animControl = getSpatial().getControl(BoatAnimationControl.class);
                        if (animControl != null) {
                            float flagFactor = animControl.getFlagFactor();
                            float sailFactor = animControl.getSailFactor();
                            float windFactor = animControl.getWindFactor();
                            animPacket = new AnimPacket(flagFactor, sailFactor, windFactor);
                        }
                    }
                    if (animPacket != null) {
                        p.getKey().send(animPacket);
                        drawPacketSent(pPos);
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error sending network update to connection " + p.getKey().getId(), e);
            }
        }
    }

    public void applyPacket(Message m) {
        log.info("Received message: " + m);
        if (m instanceof TransformPacket) {
            TransformPacket packet = (TransformPacket) m;
            if (packet.getTimestamp().isBefore(lastReceivedTransformPacket)) {
                log.finer("Received old packet");
                return;
            }
            Spatial boat = getSpatial();
            Vector3f parentPosition = boat.getParent().getWorldTranslation();
            Vector3f localPosition = boat.getLocalTranslation();
            localPosition.set(packet.getTransform().getTranslation());
            localPosition.subtractLocal(parentPosition);
            boat.setLocalTranslation(localPosition);

            Quaternion parentRotation = boat.getParent().getWorldRotation();
            Quaternion localRotation = boat.getLocalRotation();
            localRotation.set(packet.getTransform().getRotation());
            localRotation.multLocal(parentRotation);
            boat.setLocalRotation(localRotation);
        } else if (m instanceof AnimPacket) {
            AnimPacket animPacket = (AnimPacket) m;
            if (animPacket.getTimestamp().isBefore(lastReceivedTransformPacket)) {
                log.finer("Received old packet");
                return;
            }
            Spatial boat = getSpatial();
            BoatAnimationControl animControl = boat.getControl(BoatAnimationControl.class);
            if (animControl != null) {
                animControl.setFlagAnim(animPacket.getFlagFactor());
                animControl.setSailAnim(animPacket.getSailFactor(), animPacket.getWindFactor());
            } else {
                log.warning("No BoatControl found for remote boat of connection: " + spatial);
            }
        } else {
            log.warning("Received unknown message type: " + m.getClass().getName());
        }
    }

    @Override
    protected void controlUpdate(float tpf) {}

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
