package org.ngengine.ads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.nostrads.protocol.types.AdSize;
import org.ngengine.nostrads.protocol.types.AdTaxonomy;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image.Format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ImmersiveAdControl extends AbstractControl implements ImmersiveAdGroup {
    private static final Logger logger = Logger.getLogger(ImmersiveAdControl.class.getName());
    private static final String[] DEFAULT_TEXTURE_KEYS = new String[]{
        "ColorMap",
        "BaseColorMap",
        "DiffuseMap"
    };

    private boolean needUpdate = true;
    private String[] textureKeys = DEFAULT_TEXTURE_KEYS;
    
    private String categoryIds = null;
    private String languages = null;
    private String context = null;
    private String priceSlot = null;
    private Material materialReplacement;
    private boolean replaceMaterial = true;
    private AssetManager assetManager;

    private transient List<ImmersiveAdSpace> adSpaces = new ArrayList<>();

    public ImmersiveAdControl(
        @Nonnull AssetManager assetManager,
        @Nullable List<AdTaxonomy.Term> categoryIds,
        @Nullable List<String> languages,
        @Nullable String priceSlot,
        @Nullable String context
    ){
        if(languages!=null) this.languages = String.join(",", languages) ;
        if(categoryIds!=null) this.categoryIds = String.join(",", categoryIds.stream().map(t -> t.id()).toList());
        this.priceSlot = priceSlot;
        this.context = context;
        
    }

    public ImmersiveAdControl(@Nonnull AssetManager assetManager){
        this.assetManager = assetManager;
 
    }

    protected ImmersiveAdControl(){}

    public void setMaterialReplacement(Material material) {
        this.materialReplacement = material;
        this.replaceMaterial = material != null;
    }

    protected Material getMaterialReplacement() {
        if(!replaceMaterial) return null;
        if(materialReplacement==null&&replaceMaterial){
            materialReplacement = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            
        }
        return materialReplacement != null ? materialReplacement.clone() : null;
    }

    protected String getSpatialProperty(Spatial sp, String key){
        String value = null;
        if(sp!=null){
            value = ImmersiveAdProperties.getProperty(sp, key);
        }
        if(value==null||value.isEmpty()){
            value = ImmersiveAdProperties.getProperty(spatial, key);
        }
        if(value==null||value.isEmpty()){
            switch(key){
                case ImmersiveAdProperties.categories: {
                    value = categoryIds;
                    break;
                }
                case ImmersiveAdProperties.languages: {
                    value = languages;
                    break;
                }
                case ImmersiveAdProperties.context: {
                    value = context;
                    break;
                }
                case ImmersiveAdProperties.priceslot: {
                    value = priceSlot;
                    break;
                }

            }
        }
        if(value!=null&&value.isEmpty()){
            value = null;
        }
        return value;
    }

    public void setTextureKeys(String... keys) {
        this.textureKeys =  keys;
    }

    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        needUpdate = true;
    }


    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(textureKeys, "textureKeys", DEFAULT_TEXTURE_KEYS);
        oc.write(categoryIds, "categoryIds", null);
        oc.write(languages, "languages", null);
        oc.write(priceSlot, "priceSlot", null);
        oc.write(context, "context", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        assetManager = im.getAssetManager();
        textureKeys = ic.readStringArray("textureKeys", DEFAULT_TEXTURE_KEYS);
        categoryIds =  ic.readString("categoryIds", null);
        languages = ic.readString("languages", null);
        priceSlot = ic.readString("priceSlot", null);        
        context = ic.readString("context", null);
        needUpdate = true;
    }


    protected void applyTexture(Spatial sp, Texture tx) {
        sp.depthFirstTraversal(sx->{
            if(!(sx instanceof Geometry))return;
            Geometry geo = (Geometry)sx;
            Material mat = geo.getMaterial();

            Material matReplacement = getMaterialReplacement();
            if(matReplacement!=null){
                geo.setMaterial(matReplacement);
                mat = matReplacement;
            }

            for(MatParam param : mat.getMaterialDef().getMaterialParams()) {
                if(param.getVarType()!=VarType.Texture2D)continue;
                String name = param.getName();
                if(!Arrays.asList(textureKeys).contains(name))continue;         
                System.out.println("Applying texture to material param: " + name + " with texture: " + tx);
                // Texture oldTexture = (Texture) param.getValue();
                // if(oldTexture!=null&&oldTexture!=tx){
                //     adSpaces.removeIf(ref -> {
                //         return  ref.getTexture() == oldTexture;
                //     });
                // }
                mat.setTexture(name, tx);
                break;                
            }            
        });
    }

    protected void prepareAdSpaces(Spatial sp, List<ImmersiveAdSpace> spaces) {
        String adspaceV = getSpatialProperty(sp, ImmersiveAdProperties.adspace);
        if (adspaceV != null) {
            AdSize size = AdSize.fromString(adspaceV);
            if (size != null) {
                try{
                 System.out.println("Creating ad space for " + sp.getName() + " with size " + size);
                    ImmersiveAdSpace adSpace = new ImmersiveAdSpace(()->sp.getWorldBound(), tx->{
                        System.out.println("Applying texture to ad space: " + tx);
                        tx.setMinFilter(Texture.MinFilter.Trilinear);
                        tx.setMagFilter(Texture.MagFilter.Bilinear);
                        tx.setWrap(Texture.WrapMode.EdgeClamp);
                        tx.setAnisotropicFilter(4);
                        tx.setName("AdTexture_" + sp.getName() + "_" + size.toString());
                        applyTexture(sp, tx);
                    }, key->getSpatialProperty(sp, key));
                    spaces.add(adSpace);  
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to create ad space for " + sp.getName() + " with size " + size, e);
                }
            }
        } else if (sp instanceof Node) {
            for (Spatial child : ((Node) sp).getChildren()) {
                prepareAdSpaces(child, spaces);
            }
        }
    }
    
 
    @Override
    protected void controlUpdate(float tpf) {
        if(needUpdate){
            needUpdate = false;
            adSpaces.clear();
            if (spatial != null) {
                prepareAdSpaces(spatial, adSpaces);                
            } else {
                logger.warning("Spatial is null, cannot update ad spaces.");
            } 
        }
      
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
   
    }

    
    public List<ImmersiveAdSpace> getSpaces() {
        return adSpaces;
    }
 
    
}
