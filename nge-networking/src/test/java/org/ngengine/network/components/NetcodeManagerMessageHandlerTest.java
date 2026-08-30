package org.ngengine.network.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.network.RemotePeer;
import org.ngengine.network.protocol.NetworkSafe;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;

public class NetcodeManagerMessageHandlerTest {

    @NetworkSafe
    public static final class TestGameMessage extends AbstractMessage {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @Test
    public void typedHandlersRunOnLogicUpdateAndCanBeUnregistered() throws Exception {
        NetcodeManagerComponent manager = new NetcodeManagerComponent();
        AtomicInteger received = new AtomicInteger();
        NetworkMessageHandler<TestGameMessage> handler =
            (source, message) -> received.addAndGet(message.getValue());
        manager.registerMessageHandler(TestGameMessage.class, handler);

        TestGameMessage first = new TestGameMessage();
        first.setValue(7);
        enqueueInbound(manager, first);
        assertEquals(0, received.get());

        manager.updateAppLogic(null, 0f);
        assertEquals(7, received.get());

        manager.unregisterMessageHandler(TestGameMessage.class, handler);
        TestGameMessage second = new TestGameMessage();
        second.setValue(11);
        enqueueInbound(manager, second);
        manager.updateAppLogic(null, 0f);
        assertEquals(7, received.get());
    }

    @SuppressWarnings("unchecked")
    private static void enqueueInbound(NetcodeManagerComponent manager, Message message) throws Exception {
        Class<?> inboundType = Arrays.stream(NetcodeManagerComponent.class.getDeclaredClasses())
            .filter(type -> "InboundMessage".equals(type.getSimpleName()))
            .findFirst()
            .orElseThrow();
        Constructor<?> constructor = inboundType.getDeclaredConstructor(RemotePeer.class, Message.class);
        constructor.setAccessible(true);
        Object inbound = constructor.newInstance(null, message);
        Field field = NetcodeManagerComponent.class.getDeclaredField("inboundMessages");
        field.setAccessible(true);
        ((Queue<Object>) field.get(manager)).add(inbound);
    }
}
