package com.sorrowmist.useless.event.client;
import com.sorrowmist.useless.client.gui.ModeWheelScreen;
import com.sorrowmist.useless.client.gui.GtToolWheelScreen;
import com.sorrowmist.useless.core.common.KeyBindings;
import com.sorrowmist.useless.utils.UselessItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
@EventBusSubscriber(Dist.CLIENT)
public class ModeWheelHandler {
 private static long pressedAt;
 private static int pressedKey;
 private static boolean shortAction;
 @SubscribeEvent public static void key(InputEvent.Key event){
  var mc=Minecraft.getInstance();if(mc.player==null)return;
  int normal=KeyBindings.SWITCH_MODE_WHEEL_KEY.get().getKey().getValue();
  int wheel=KeyBindings.GT_TOOL_WHEEL_KEY.get().getKey().getValue();
  if(event.getAction()==1 && (event.getKey()==normal||event.getKey()==wheel)){
   if(mc.screen instanceof ModeWheelScreen && event.getKey()==normal){mc.setScreen(null);return;}
   if(mc.screen!=null||UselessItemUtils.findTargetToolInHands(mc.player).isEmpty())return;
   pressedKey=event.getKey();shortAction=pressedKey==normal;pressedAt=net.minecraft.Util.getMillis();
  }else if(event.getAction()==0 && pressedAt!=0 && event.getKey()==pressedKey){
   pressedAt=0;
   if(shortAction && mc.screen==null)UselessItemUtils.findTargetToolInHands(mc.player).ifPresent(entry->mc.setScreen(new ModeWheelScreen(entry.getKey())));
  }
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END||pressedAt==0)return;
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.screen!=null||!mc.isWindowActive()){pressedAt=0;return;}
  if(pressedKey==KeyBindings.GT_TOOL_WHEEL_KEY.get().getKey().getValue() && net.minecraft.Util.getMillis()-pressedAt>=250){pressedAt=0;mc.setScreen(new GtToolWheelScreen(pressedKey));}
 }
}
