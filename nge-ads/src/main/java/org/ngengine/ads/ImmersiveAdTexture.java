// package org.ngengine.ads;

// import java.io.ByteArrayInputStream;
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.List;

// import org.ngengine.nostrads.protocol.AdBidEvent;
// import org.ngengine.nostrads.protocol.types.AdMimeType;
// import org.ngengine.nostrads.protocol.types.AdSize;
// import org.ngengine.platform.NGEPlatform;
// import org.ngengine.runner.Runner;

// import com.jme3.asset.AssetManager;
// import com.jme3.asset.TextureKey;
// import com.jme3.texture.Image;
// import com.jme3.texture.Image.Format;
// import com.jme3.texture.Texture;
// import com.jme3.util.BufferUtils;
// import com.jme3.texture.Texture2D;

// public class ImmersiveAdTexture extends Texture2D{
//     private final AdSize size;
//     private final List<AdMimeType> supportedMimeTypes = new ArrayList<>();
    
//     public ImmersiveAdTexture(AdSize size, AdMimeType... supportedMimeTypes) {
//         super(size.getWidth(),size.getHeight(),Format.RGB8);
//         this.size = size;
//         for (AdMimeType mimeType : supportedMimeTypes) {
//             this.supportedMimeTypes.add(mimeType);
//         }
//         getImage().setData(BufferUtils.createByteBuffer(size.getWidth() * size.getHeight() * Format.RGB8.getBitsPerPixel() / 8));
//         setMinFilter(MinFilter.Trilinear);
//         setMagFilter(MagFilter.Bilinear);
//     }

//     public AdSize getSize() {
//         return size;
//     }

//     public List<AdMimeType> getSupportedMimeTypes() {
//         return supportedMimeTypes;
//     }

//     public void set(AssetManager am, AdBidEvent bid, Runner runner){
//         AdMimeType mimeType = bid.getMIMEType();
//         if(!supportedMimeTypes.contains(mimeType)){
//             throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
//         }

//         switch(mimeType){
//             case IMAGE_JPEG:
//             case IMAGE_PNG: {
//                 String url = bid.getPayload();
//                 NGEPlatform.get().httpRequest("GET", url, null, null, null).then(res->{
//                     byte[] data = res.body();
//                     try(ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
//                         TextureKey k = new TextureKey(url);
//                         Texture tx = am.loadAssetFromStream(k, bais);
//                         Image img = tx.getImage();
//                         runner.run(()->{
//                             setImage(img);
//                         });                                     
//                     } catch (Exception e) {
//                         e.printStackTrace();
//                     }                   
//                     return null;
//                 });
//                 break;
//             }
//             case TEXT_PLAIN:{

//                 break;
//             }
//             default: {
//                 throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
//             }
//         }
        

//     }
    
// }
