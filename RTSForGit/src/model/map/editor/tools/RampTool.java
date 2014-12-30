/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model.map.editor.tools;

import java.util.ArrayList;
import model.map.Ramp;
import model.map.Tile;
import model.map.cliff.Cliff;
import model.map.editor.MapToolManager;
import model.map.editor.Pencil;
import tools.LogUtil;

/**
 *
 * @author Benoît
 */
public class RampTool extends MapTool{

    public RampTool(MapToolManager manager, Pencil selector) {
        super(manager, selector);
    }

    @Override
    public void primaryAction() {
        Tile t = pencil.getCenterTile();
        if(t.ramp != null){
            t.ramp.grow(t);
        } else {
            if(!t.isCliff)
                return;
            new Ramp(t, manager.map);
        }
        
        ArrayList<Tile> changed = new ArrayList<>();
        changed.addAll(t.ramp.tiles);
        for(Tile t1 : t.ramp.tiles)
            for(Tile n : t1.get8Neighbors()){
                if(!changed.contains(n))
                    changed.add(n);
                if(n.isCliff)
                    n.unsetCliff();
            }
        manager.updateTiles(changed);
    }

    @Override
    public void secondaryAction() {
    }

    @Override
    public void toggleSet() {
        LogUtil.logger.info("Unavailable for ramp tool.");
    }

    @Override
    public void toggleOperation() {
        LogUtil.logger.info("Unavailable for ramp tool.");
    }
    
}