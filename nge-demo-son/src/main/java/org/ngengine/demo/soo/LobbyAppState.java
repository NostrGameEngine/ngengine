package org.ngengine.demo.soo;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.ngengine.network.Lobby;
import org.ngengine.network.LobbyManager;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.SpringGridLayout;

public class LobbyAppState extends BaseAppState{
    private LobbyManager mng;

    public LobbyAppState(Node guiNode) {
    }

    @Override
    protected void initialize(Application app) {
        mng = new LobbyManager(Settings.SIGNER, Settings.GAME_NAME, Settings.GAME_VERSION, Settings.RELAYS, Settings.TURN_SERVER);
        
    }

    public List<Lobby> list(String words, int limit){
        try {
            return mng.listLobbies(words, limit, null);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    protected void cleanup(Application app) {
       
    }

    @Override
    protected void onEnable() {
        ViewPort guiViewport = getApplication().getGuiViewPort();
        Node guiNode = (Node) guiViewport.getScenes().get(0);

        int width = guiViewport.getCamera().getWidth();
        int height = guiViewport.getCamera().getHeight();
        int margins = 10;

        Container myWindow = new Container();
        guiNode.attachChild(myWindow);

     
        myWindow.setLocalTranslation(margins, height-margins, 0);
        myWindow.setPreferredSize(new Vector3f(width-margins*2, height-margins*2, 0));
        myWindow.setLayout(new BorderLayout());


        Container centerPanel = new Container(new BoxLayout(Axis.X, FillMode.Proportional));
        Button join = new Button("+ New");
        Button refresh = new Button("Refresh");
     

        Container buttonPanel = new Container(new BoxLayout());
        buttonPanel.addChild(join);
        buttonPanel.addChild(refresh);

        ListBox<String> listBox = new ListBox<>();
        listBox.setPreferredSize(new Vector3f(width-margins*2, 1, 0));
        // listBox.setPreferredSize(new Vector3f(width-margins*30, height-margins*2*4, 0));



        centerPanel.addChild(listBox);
        centerPanel.addChild(buttonPanel);

        
        Container searchBar = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last,
                FillMode.Even));

        Label searchLabel = new Label("Search:");
        searchBar.addChild(searchLabel);
        searchLabel.setTextVAlignment(VAlignment.Center);
        

        TextField search = new TextField("");
        search.setTextVAlignment(VAlignment.Center);

        search.setName("search");
        searchBar.addChild(search);
        
        Label title = new Label("Find a game");
        title.setTextHAlignment(HAlignment.Center);
        title.setTextVAlignment(VAlignment.Center);
        title.setFontSize(margins*2);
        myWindow.addChild(searchBar, BorderLayout.Position.North);
       
        myWindow.addChild(centerPanel, BorderLayout.Position.Center);



        refresh.addClickCommands((src) -> {
            listBox.getModel().clear();
            List<Lobby> lobbies = list("", 10);
            for (Lobby lobby : lobbies) {
                listBox.getModel().add(lobby.toString());
            }

        });

        join.addClickCommands((src) -> {
           try {
            mng.createLobby("", Map.of(), Duration.ofHours(1));
           } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
           }
        });
    }

    @Override
    protected void onDisable() {
        
    }
    
}
