package io.github.jmecn.tiled.core;

public interface UpdateListener {
     public interface UpdateFlag {
        public int SHAPE    = 1 << 0; 
        public int TRANSFORM = 1 << 1;  
        public int PROPERTIES = 1 << 2;
     }

    public void onUpdateNeeded(Base entry);
}
