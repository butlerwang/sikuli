package edu.mit.csail.uid;

import java.awt.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import com.wapmx.nativeutils.jniloader.NativeLoader;

public class EventManager {
   final static int APPEAR = 0;
   final static int VANISH = 1;
   final static int CHANGE = 2;
   private static EventManager _instance = null;
   private long _c_instance = 0;
   private Map<Integer, SikuliEventObserver> _obMap;
   private Region _region;

   static {
      try{
         NativeLoader.loadLibrary("ScreenMatchProxy");
         System.out.println("ScreenMatchProxy loaded.");
      }
      catch(IOException e){
         e.printStackTrace();
      }
   }

   /*
   public static EventManager getInstance(){
      if(_instance==null)
         _instance = new EventManager();
      return _instance;
   }
   */

   public EventManager(Region region){
      _c_instance = createEventManager();
      _obMap = new HashMap<Integer, SikuliEventObserver>();
      _region = region;
   }

   private <PSC> String getFilename(PSC ptn){
      if( ptn instanceof Pattern ){
         return ((Pattern)ptn).imgURL;
      }
      else if( ptn instanceof String){
         return (String)ptn;
      }
      return null;
   }

   private int getObserverId(SikuliEventObserver ob){
      int id = ob.hashCode();
      while(_obMap.containsKey(id)){
         id+=(int)(Math.random()*100);
      }
      _obMap.put(id, ob);
      return id;
   }

   private SikuliEventObserver getObserverFromId(int id){
      return _obMap.get(id);
   }

   public <PSC> void addAppearObserver(PSC ptn, SikuliEventObserver ob){
      int handler_id = getObserverId(ob);
      addObserver(_c_instance, APPEAR, getFilename(ptn), handler_id,
                  0, 0, _region.w, _region.h);
   }

   public <PSC> void addVanishObserver(PSC ptn, SikuliEventObserver ob){
      int handler_id = getObserverId(ob);
      addObserver(_c_instance, VANISH, getFilename(ptn), handler_id,
                  0, 0, _region.w, _region.h);
   
   }

   public void addChangeObserver(SikuliEventObserver ob){
      int handler_id = getObserverId(ob);
      addObserver(_c_instance, CHANGE, "", handler_id,
                  0, 0, _region.w, _region.h);
   }

   public void update(ScreenImage img){
      byte[] data = OpenCV.convertBufferedImageToByteArray(img.getImage());
      SikuliEvent[] events = _update(_c_instance, data, img.w, img.h);
      if(events == null)
         return;
      for(SikuliEvent e : events){
         SikuliEventObserver ob = getObserverFromId(e.handler_id);
         e.x += _region.x;
         e.y += _region.y;
         switch(e.type){
            case APPEAR:
               ob.targetAppeared(new AppearEvent(e));
               break;
            case VANISH:
               ob.targetVanished(new VanishEvent(e));
               break;
            case CHANGE:
               ob.targetChanged(new ChangeEvent(e));
               break;
         }
      }
   }

   private native long createEventManager(); 
   private native void addObserver(long sem_instance, 
                                   int evt_type, String target_image_filename,
                                   int handler_id, int x, int y, int w, int h); 

   private native SikuliEvent[] _update(long sem_instance, 
                                        byte[] screenImage, int w, int h);

}

