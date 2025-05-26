package com.jme3.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SavableWrapSerializable implements Savable  {

 
    protected Serializable obj;

    public SavableWrapSerializable() {
    }

    public void set(Serializable obj) {
        this.obj = obj;
    }

    public <T extends Serializable> T get() {
        return (T) obj;
    }

    public SavableWrapSerializable(Serializable obj) {
        this.obj = obj;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try{
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            byte[] bytes = baos.toByteArray();
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(bytes, "obj", null);
            
        } finally {
            oos.close();            
        }

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        try{
            InputCapsule capsule = im.getCapsule(this);
            byte[] bytes = capsule.readByteArray("obj", null);
            if (bytes != null) {
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                    obj = (Serializable) ois.readObject();
                    ois.close();
                } finally {
                    if(ois!=null)ois.close();
                }
            }
        } catch (ClassNotFoundException ex) {
            throw new IOException("Class not found", ex);
        }  
    }
  
    
}
