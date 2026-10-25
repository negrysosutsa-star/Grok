package ft.triggerbot.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ft.triggerbot.ClientMain;
import ft.triggerbot.module.AimAssistModule;
import ft.triggerbot.module.TriggerBotModule;

public class ClickGui extends Screen {
    private final ClientMain c = ClientMain.INSTANCE;
    private final TriggerBotModule tb = c.triggerBot;
    private final AimAssistModule aa = c.aimAssist;
    private boolean listenTb, listenAa;

    public ClickGui() { super(Text.literal("FT Client")); }

    @Override
    protected void init() {
        int cx = width/2, y = 24, w = 220, h = 16, g = 18;

        addDrawableChild(b(cx,y,w,h,"TriggerBot: "+on(tb.enabled), btn -> { tb.toggle(); btn.setMessage(Text.literal("TriggerBot: "+on(tb.enabled))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"TB Bind: "+ClientMain.keyName(tb.key), btn -> { listenTb=true; listenAa=false; btn.setMessage(Text.literal("TB Bind: ...")); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"Crit: "+tb.critMode, btn -> { tb.critMode="smart".equals(tb.critMode)?"only_crits":"smart"; btn.setMessage(Text.literal("Crit: "+tb.critMode)); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"TB Players: "+on(tb.onlyPlayers), btn -> { tb.onlyPlayers=!tb.onlyPlayers; btn.setMessage(Text.literal("TB Players: "+on(tb.onlyPlayers))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"TB Weapon: "+on(tb.weaponOnly), btn -> { tb.weaponOnly=!tb.weaponOnly; btn.setMessage(Text.literal("TB Weapon: "+on(tb.weaponOnly))); })); y+=g;
        addDrawableChild(new IS(cx-w/2,y,w,h,"MinDelay",tb.minDelay,0,15,v->tb.minDelay=v)); y+=g;
        addDrawableChild(new IS(cx-w/2,y,w,h,"MaxDelay",tb.maxDelay,0,15,v->tb.maxDelay=Math.max(v,tb.minDelay))); y+=g;
        addDrawableChild(new FS(cx-w/2,y,w,h,"TB Range",tb.range,1,6,v->tb.range=v)); y+=g+4;

        addDrawableChild(b(cx,y,w,h,"AimAssist: "+on(aa.enabled), btn -> { aa.toggle(); btn.setMessage(Text.literal("AimAssist: "+on(aa.enabled))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"AA Bind: "+ClientMain.keyName(aa.key), btn -> { listenAa=true; listenTb=false; btn.setMessage(Text.literal("AA Bind: ...")); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"AA Players: "+on(aa.onlyPlayers), btn -> { aa.onlyPlayers=!aa.onlyPlayers; btn.setMessage(Text.literal("AA Players: "+on(aa.onlyPlayers))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"AA Weapon: "+on(aa.weaponOnly), btn -> { aa.weaponOnly=!aa.weaponOnly; btn.setMessage(Text.literal("AA Weapon: "+on(aa.weaponOnly))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"AA Walls: "+on(aa.wallCheck), btn -> { aa.wallCheck=!aa.wallCheck; btn.setMessage(Text.literal("AA Walls: "+on(aa.wallCheck))); })); y+=g;
        addDrawableChild(b(cx,y,w,h,"AA OnlyX: "+on(aa.onlyOnX), btn -> { aa.onlyOnX=!aa.onlyOnX; btn.setMessage(Text.literal("AA OnlyX: "+on(aa.onlyOnX))); })); y+=g;
        addDrawableChild(new FS(cx-w/2,y,w,h,"AA Range",aa.range,1,6,v->aa.range=v)); y+=g;
        addDrawableChild(new FS(cx-w/2,y,w,h,"AA FOV",aa.fov,20,360,v->aa.fov=v)); y+=g;
        addDrawableChild(new FS(cx-w/2,y,w,h,"AA Speed",aa.speed,1,40,v->aa.speed=v)); y+=g;
        addDrawableChild(new FS(cx-w/2,y,w,h,"AA Hold",aa.holdAfterHit,0,20,v->aa.holdAfterHit=v)); y+=g+6;

        addDrawableChild(b(cx,y,w,h,"Close", btn -> close()));
    }

    private ButtonWidget b(int cx,int y,int w,int h,String t,ButtonWidget.PressAction a){
        return ButtonWidget.builder(Text.literal(t),a).dimensions(cx-w/2,y,w,h).build();
    }
    private static String on(boolean v){ return v?"ON":"OFF"; }

    @Override public boolean keyPressed(int k,int s,int m){
        if(listenTb){ tb.key=k==GLFW.GLFW_KEY_ESCAPE?0:k; listenTb=false; clearAndInit(); return true; }
        if(listenAa){ aa.key=k==GLFW.GLFW_KEY_ESCAPE?0:k; listenAa=false; clearAndInit(); return true; }
        if(k==GLFW.GLFW_KEY_INSERT||k==GLFW.GLFW_KEY_ESCAPE){ close(); return true; }
        return super.keyPressed(k,s,m);
    }

    @Override public void render(DrawContext c,int mx,int my,float d){
        renderBackground(c,mx,my,d);
        c.drawCenteredTextWithShadow(textRenderer,title,width/2,6,0xFFFFFF);
        if(listenTb||listenAa) c.drawCenteredTextWithShadow(textRenderer,Text.literal("press key ESC=none"),width/2,height-14,0xFFAA00);
        super.render(c,mx,my,d);
    }
    @Override public boolean shouldPause(){ return false; }

    private class IS extends SliderWidget {
        final String l; final int min,max; final java.util.function.IntConsumer set;
        IS(int x,int y,int w,int h,String l,int v,int min,int max,java.util.function.IntConsumer s){
            super(x,y,w,h,Text.empty(),(v-min)/(double)(max-min)); this.l=l; this.min=min; this.max=max; set=s; updateMessage();
        }
        @Override protected void updateMessage(){ setMessage(Text.literal(l+": "+(int)Math.round(min+value*(max-min)))); }
        @Override protected void applyValue(){ set.accept((int)Math.round(min+value*(max-min))); }
    }
    private class FS extends SliderWidget {
        final String l; final float min,max; final java.util.function.DoubleConsumer set;
        FS(int x,int y,int w,int h,String l,float v,float min,float max,java.util.function.DoubleConsumer s){
            super(x,y,w,h,Text.empty(),(v-min)/(max-min)); this.l=l; this.min=min; this.max=max; set=s; updateMessage();
        }
        @Override protected void updateMessage(){ setMessage(Text.literal(l+": "+String.format("%.1f",(float)(min+value*(max-min))))); }
        @Override protected void applyValue(){ set.accept((double)(min+value*(max-min))); }
    }
}