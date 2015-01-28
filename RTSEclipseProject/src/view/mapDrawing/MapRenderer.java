package view.mapDrawing;


import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Quaternion;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import math.Angle;
import model.battlefield.map.Trinket;
import model.battlefield.map.cliff.Cliff;
import model.battlefield.map.Tile;
import static model.battlefield.map.cliff.Cliff.Type.Corner;
import static model.battlefield.map.cliff.Cliff.Type.Orthogonal;
import static model.battlefield.map.cliff.Cliff.Type.Salient;
import model.battlefield.map.cliff.faces.manmade.ManmadeFace;
import model.battlefield.map.cliff.faces.natural.NaturalFace;
import model.battlefield.map.parcel.ParcelMesh;
import tools.LogUtil;
import view.View;
import view.jme.TerrainSplatTexture;
import view.material.MaterialManager;
import view.math.Translator;

public class MapRenderer implements ActionListener {

    View view;
    MaterialManager mm;
    AssetManager am;

    private HashMap<String, Spatial> models = new HashMap<>();

    private HashMap<ParcelMesh, Spatial> parcelsSpatial = new HashMap<>();
    private HashMap<Tile, Spatial> tilesSpatial = new HashMap<>();

    public TerrainSplatTexture groundTexture;

    public Node mainNode = new Node();
    public Node castAndReceiveNode = new Node();
    public Node receiveNode = new Node();
    
    public PhysicsSpace mainPhysicsSpace = new PhysicsSpace();
    
    public MapRenderer(View view, MaterialManager mm, AssetManager am) {
        this.view = view;
        groundTexture = new TerrainSplatTexture(view.model.battlefield.map.atlas, am);
        this.mm = mm;
        this.am = am;
        castAndReceiveNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        receiveNode.setShadowMode(RenderQueue.ShadowMode.Receive);
        mainNode.attachChild(castAndReceiveNode);
        mainNode.attachChild(receiveNode);
    }
	
    public void renderTiles() {
            LogUtil.logger.info("rendering ground"+view.model.battlefield.parcelManager.meshes.size()+" "+view.model.battlefield.map.tiles.size());
            int index = 0;
            for(String s : view.model.battlefield.map.style.textures){
                Texture diffuse = am.loadTexture(s);
                Texture normal;
                if(view.model.battlefield.map.style.normals.get(index) != null)
                    normal = am.loadTexture(view.model.battlefield.map.style.normals.get(index));
                else
                    normal = null;
                double scale = view.model.battlefield.map.style.scales.get(index);
                groundTexture.addTexture(diffuse, normal, scale);
                index++;
            }
            groundTexture.buildMaterial();
            
            for(ParcelMesh mesh : view.model.battlefield.parcelManager.meshes){
                Geometry g = new Geometry();
                Mesh jmeMesh = Translator.toJMEMesh(mesh);
                TangentBinormalGenerator.generate(jmeMesh);
                g.setMesh(jmeMesh);
                g.setMaterial(groundTexture.getMaterial());
                g.setQueueBucket(Bucket.Transparent);
                
                g.addControl(new RigidBodyControl(0));
                parcelsSpatial.put(mesh, g);
                castAndReceiveNode.attachChild(g);
                mainPhysicsSpace.add(g);
            }
            updateTiles(view.model.battlefield.map.tiles);
    }

    private Spatial getModel(String path){
        if(!models.containsKey(path))
            models.put(path, am.loadModel(path));
        return models.get(path).clone();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch(e.getActionCommand()){
            case "parcels" : updateParcelsFor((ArrayList<ParcelMesh>)(e.getSource())); break;
            case "tiles" : updateTiles((ArrayList<Tile>)(e.getSource())); break;
            case "ground" : updateGroundTexture(); break;
        }
    }
    
    private void updateGroundTexture(){
        groundTexture.getMaterial();
    }
    
    private void updateTiles(List<Tile> tiles){
        for(Tile t : tiles){
            freeTileNode(t);
            for(Cliff c : t.getCliffs()){
                if(c.type == Cliff.Type.Bugged)
                    attachBuggedCliff(t);
                else if(c.face == null)
                    continue;
                else if(c.face.getType().equals("natural"))
                    attachNaturalCliff(t, c);
                else if(c.face.getType().equals("manmade"))
                    attachManmadeCliff(t, c);
            }
        }
    }
    
    private void freeTileNode(Tile t){
        Node n = (Node)tilesSpatial.get(t);
        if(n != null)
            castAndReceiveNode.detachChild(n);
        tilesSpatial.remove(t);
    }
    
    private void attachBuggedCliff(Tile t){
        Geometry g = new Geometry();
        g.setMesh(new Box(0.5f, 0.5f, 1));
        g.setMaterial(mm.redMaterial);
        g.setLocalTranslation(t.x+0.5f, t.y+0.5f, (float)(t.level*Tile.STAGE_HEIGHT)+1);
        
        Node n = new Node();
        n.attachChild(g);
        tilesSpatial.put(t, n);
        castAndReceiveNode.attachChild(n);
    }
    
    private void attachNaturalCliff(Tile t, Cliff c){
        Node n = new Node();
        tilesSpatial.put(t, n);
        castAndReceiveNode.attachChild(n);

        NaturalFace face = (NaturalFace)(c.face);
        Geometry g = new Geometry();
        g.setMesh(Translator.toJMEMesh(face.mesh));
        if(face.color != null)
            g.setMaterial(mm.getLightingColor(Translator.toColorRGBA(face.color)));
        else
            g.setMaterial(mm.getLightingTexture(face.texturePath));
//            g.setMaterial(mm.getLightingTexture("textures/road.jpg"));
        g.rotate(0, 0, (float)(c.angle));
        g.setLocalTranslation(t.x+0.5f, t.y+0.5f, (float)(c.level*Tile.STAGE_HEIGHT));
        n.attachChild(g);
    }
    
    private void attachManmadeCliff(Tile t, Cliff c){
        Node n = new Node();
        tilesSpatial.put(t, n);
        castAndReceiveNode.attachChild(n);

        ManmadeFace face = (ManmadeFace)(c.face);
        Spatial s = getModel(face.modelPath);
        if(s == null){
            LogUtil.logger.warning("Can't find model "+face.modelPath);
            return;
        }
        switch (c.type){
            case Orthogonal : 
                s.rotate(0, 0, (float) (c.angle+Angle.RIGHT));
                break;
            case Salient : 
                s.rotate(0, 0, (float)(c.angle+Angle.RIGHT));
                break;
            case Corner : 
                s.rotate(0, 0, (float)(c.angle));
                break;
        }
        s.scale(0.005f);
        s.setLocalTranslation(t.x+0.5f, t.y+0.5f, (float)(c.level*Tile.STAGE_HEIGHT)+0.1f);
        n.attachChild(s);
    }
    
    private void updateParcelsFor(List<ParcelMesh> toUpdate){
        for(ParcelMesh parcel : toUpdate){
            Mesh jmeMesh = Translator.toJMEMesh(parcel);
            TangentBinormalGenerator.generate(jmeMesh);
            ((Geometry)parcelsSpatial.get(parcel)).setMesh(jmeMesh);
        }
    }
}