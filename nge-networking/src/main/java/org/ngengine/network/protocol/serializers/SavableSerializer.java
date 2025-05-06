
package org.ngengine.network.protocol.serializers;

import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ngengine.network.protocol.GrowableByteBuffer;

public class SavableSerializer extends DynamicSerializer {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();
        byte[] bytes = new byte[length];
        data.get(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        BinaryImporter importer = BinaryImporter.getInstance();
        Savable s = importer.load(bais);
        bais.close();

        return (T) s;
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Savable s = (Savable) object;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryExporter exporter = BinaryExporter.getInstance();
        exporter.save(s, baos);

        byte[] data = baos.toByteArray();
        baos.close();

        buffer.putInt(data.length);
        buffer.put(data);

    }

}
