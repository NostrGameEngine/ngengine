package org.ngengine;

import java.io.Closeable;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.ngengine.runner.MainThreadRunner;
import org.ngengine.runner.PassthroughRunner;
import org.ngengine.runner.Runner;

import com.jme3.app.Application;
import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.FilterKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Caps;
import com.jme3.scene.Spatial;
import com.jme3.shader.ShaderGenerator;
import com.jme3.texture.Texture;

public class AsyncAssetManager implements AssetManager,Closeable {
    private static final Map<AssetManager, AsyncAssetManager> asyncManagers = new WeakHashMap<>();
    protected final AssetManager assetManager;
     private final ExecutorService assetLoaderThread = Executors.newFixedThreadPool(1,(r)->{
        Thread t = new Thread(r,"AssetLoaderThread");
        t.setDaemon(true);
        return t;
     });
     
    private Runner callbackRunner;

    public static AsyncAssetManager of(AssetManager assetManager, Application app) {
        if (assetManager == null) {
            throw new IllegalArgumentException("AssetManager cannot be null");
        }
        return asyncManagers.computeIfAbsent(assetManager, (am) -> new AsyncAssetManager(am, MainThreadRunner.of(app)));
    }

    @Override
    public void close(){
        assetLoaderThread.shutdown();
        asyncManagers.remove(assetManager);
    }

    protected AsyncAssetManager(AssetManager assetManager) {
        this(assetManager, new PassthroughRunner());
    }

    protected AsyncAssetManager(AssetManager assetManager, Runner callbackRunner) {
        this.assetManager = assetManager;
        this.callbackRunner = callbackRunner;
    }

    public <T> void runInLoaderThread(Function<AsyncAssetManager, T> function, BiConsumer<T, Throwable> callback) {
        assetLoaderThread.execute(()->{
            try{
                T res = function.apply(this);
                callbackRunner.run(() -> {
                    callback.accept(res, null);
                });
             } catch (Throwable ex) {
                callbackRunner.run(() -> {
                    callback.accept(null, ex);
                });
            }
        });
    }

    ///
   
    protected <T,E> void async(E key, Function<E,T> function, BiConsumer<T, Throwable> callback) {
        assetLoaderThread.execute(() -> {
            try {
                T result = function.apply(key);
                callbackRunner.run(() -> {
                    callback.accept(result, null);
                });
            } catch (Throwable ex) {
                callbackRunner.run(() -> {
                    callback.accept(null, ex);
                });
            }
        });
     }


    public <T> void loadAssetAsync(AssetKey<T> key, BiConsumer<T, Throwable> callback) {       
        async(key, assetManager::loadAsset, callback);
    }

    @Override
    public <T> T loadAsset(AssetKey<T> key) {
        return assetManager.loadAsset(key);
    }


    public  void loadAssetAsync(String name, BiConsumer<Object, Throwable> callback) {
    
        async(name, assetManager::loadAsset, callback);

         
    }

    @Override
    public Object loadAsset(String name) {
        return assetManager.loadAsset(name);
    }

    public void loadTextureAsync(TextureKey key, BiConsumer<Texture, Throwable> callback) {
        async(key, assetManager::loadTexture, callback);
    }

    @Override
    public Texture loadTexture(TextureKey key) {
        return assetManager.loadTexture(key);
    }

    public void loadTextureAsync(String name, BiConsumer<Texture, Throwable> callback) {
        async(name, assetManager::loadTexture, callback);       
    }

    @Override
    public Texture loadTexture(String name) {
        return assetManager.loadTexture(name);
    }

    public void loadAudioAsync(AudioKey key ,BiConsumer<AudioData, Throwable> callback) {
         async(key, assetManager::loadAudio, callback);         
    }

    @Override
    public AudioData loadAudio(AudioKey key) {
        return assetManager.loadAudio(key);
    }

    public void loadAudioAsync(String name, BiConsumer<AudioData, Throwable> callback) {
        async(name, assetManager::loadAudio, callback);      
    }


    @Override
    public AudioData loadAudio(String name) {
        return assetManager.loadAudio(name);
    }

    public void loadModelAsync(ModelKey key, BiConsumer<Spatial, Throwable> callback) {
        async(key, assetManager::loadModel, callback);       
    }

    @Override
    public Spatial loadModel(ModelKey key) {
        return assetManager.loadModel(key);
    }


    public void loadModelAsync(String name, BiConsumer<Spatial, Throwable> callback) {
        async(name, assetManager::loadModel, callback);
      
    }

    @Override
    public Spatial loadModel(String name) {
        return assetManager.loadModel(name);
    }

    public void loadMaterialAsync(String name, BiConsumer<Material, Throwable> callback) {
        async(name, assetManager::loadMaterial, callback);        
    }

    @Override
    public Material loadMaterial(String name) {
        return assetManager.loadMaterial(name);
    }

    public void loadFontAsync(String name, BiConsumer<BitmapFont, Throwable> callback) {
        async(name, assetManager::loadFont, callback);
      
    }

    @Override
    public BitmapFont loadFont(String name) {
        return assetManager.loadFont(name);
    }


    public void loadFilterAsync(FilterKey key,
            BiConsumer<FilterPostProcessor, Throwable> callback) {
        async(key, assetManager::loadFilter, callback);
    }

    @Override
    public FilterPostProcessor loadFilter(FilterKey key) {
        return assetManager.loadFilter(key);
    }

    public void loadFilterAsync(String name,
            BiConsumer<FilterPostProcessor, Throwable> callback) {
        async(name, assetManager::loadFilter, callback);
    }

    @Override
    public FilterPostProcessor loadFilter(String name) {
        return assetManager.loadFilter(name);
    }

     public void locateAssetAsync(AssetKey<?> key,
             BiConsumer<AssetInfo, Throwable> callback) {
        async(key, assetManager::locateAsset, callback);
    }

    @Override
    public AssetInfo locateAsset(AssetKey<?> key) {
        return assetManager.locateAsset(key);
    }

    public <T> void loadAssetFromStreamAsync(AssetKey<T> key, InputStream inputStream, 
            BiConsumer<T, Throwable> callback) {
        async(key, (k) -> assetManager.loadAssetFromStream(k, inputStream), callback);
    }

    @Override
    public <T> T loadAssetFromStream(AssetKey<T> key, InputStream inputStream) {
        return assetManager.loadAssetFromStream(key, inputStream);
    }

    /////


    @Override
    public void registerLoader(Class<? extends AssetLoader> loaderClass, String... extensions) {
        assetManager.registerLoader(loaderClass, extensions);
    }

    @Override
    public void unregisterLoader(Class<? extends AssetLoader> loaderClass) {
        assetManager.unregisterLoader(loaderClass);
    }

    @Override
    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        assetManager.registerLocator(rootPath, locatorClass);
    }

    @Override
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        assetManager.unregisterLocator(rootPath, locatorClass);
    }

    @Override
    public void addAssetEventListener(AssetEventListener listener) {

        assetManager.addAssetEventListener(listener);
    }

    @Override
    public void removeAssetEventListener(AssetEventListener listener) {

        assetManager.removeAssetEventListener(listener);
    }

    @Override
    public void clearAssetEventListeners() {
        assetManager.clearAssetEventListeners();
    }

    @Override
    public void setShaderGenerator(ShaderGenerator generator) {
        assetManager.setShaderGenerator(generator);
    }

    @Override
    public ShaderGenerator getShaderGenerator(EnumSet<Caps> caps) {
        return assetManager.getShaderGenerator(caps);
    }

    @Override
    public <T> T getFromCache(AssetKey<T> key) {
        return assetManager.getFromCache(key);
    }

    @Override
    public <T> void addToCache(AssetKey<T> key, T asset) {
        assetManager.addToCache(key, asset);
    }

    @Override
    public <T> boolean deleteFromCache(AssetKey<T> key) {
        return assetManager.deleteFromCache(key);
    }

    @Override
    public void clearCache() {
        assetManager.clearCache();
    }

}
