package net.sf.odinms.scripting.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.scripting.AbstractScriptManager;
import net.sf.odinms.tools.Pair;

public class NPCScriptManager extends AbstractScriptManager {

    private Map<MapleClient, NPCConversationManager> cms = new HashMap<MapleClient, NPCConversationManager>();
    private Map<MapleClient, NPCScript> scripts = new HashMap<MapleClient, NPCScript>();
    private static NPCScriptManager instance = new NPCScriptManager();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc) {
        start(c, npc, null, null);
    }

    public void start(MapleClient c, int npc, String filename, MapleCharacter chr) {
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc, chr, filename);
            if (cms.containsKey(c)) {
                dispose(c);
                return;
            }
            cms.put(c, cm);
            Invocable iv = getInvocable("npc/" + npc + ".js", c);
            if (filename != null) {
                iv = getInvocable("npc/" + filename + ".js", c);
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv == null) {
                    cm.sendOk("Hi I'm an uncoded npc.\r\nMy ID is #r" + npc + "#k.");
                }
                cm.dispose();
                return;
            }
            addNpcTalkTimes(c.getPlayer().getId(), npc);
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            ns.start();
        } catch (Exception e) {
            log.error("Error executing NPC script.", e);
            dispose(c);
            cms.remove(c);
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (Exception e) {
                log.error("Error executing NPC script.", e);
                dispose(c);
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        cms.remove(cm.getC());
        scripts.remove(cm.getC());
        if (cm.getFileName() != null) {
            resetContext("npc/" + cm.getFileName() + ".js", cm.getC());
        } else {
            resetContext("npc/" + cm.getNpc() + ".js", cm.getC());
        }
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        if (npccm != null) {
            dispose(npccm);
        }
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }
    private Map<Pair<Integer, Integer>, Integer> npcTalk = new HashMap<Pair<Integer, Integer>, Integer>();

    public int getNpcTalkTimes(int chrid, int npc) {
        Pair<Integer, Integer> pplayer = new Pair<Integer, Integer>(chrid, npc); // first time <3 looks wrong.
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        return npcTalk.get(pplayer);
    }

    public void addNpcTalkTimes(int chrid, int npc) {
        Pair<Integer, Integer> pplayer = new Pair<Integer, Integer>(chrid, npc);
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        int talk = 1 + npcTalk.get(pplayer);
        npcTalk.remove(pplayer);
        npcTalk.put(pplayer, talk);
    }

    public void setNpcTalkTimes(int chrid, int npc, int amount) {
        Pair<Integer, Integer> pplayer = new Pair<Integer, Integer>(chrid, npc);
        if (!npcTalk.containsKey(pplayer)) {
            npcTalk.put(pplayer, 0);
        }
        npcTalk.remove(pplayer);
        npcTalk.put(pplayer, amount);
    }

    public List<Integer> listTalkedNpcsByID(int chrid) {
        List<Integer> npcs = new ArrayList<Integer>();
        for (Pair<Integer, Integer> rawr : npcTalk.keySet()) {
            if (rawr.getLeft().equals(chrid)) {
                npcs.add(rawr.getRight());
            }
        }
        return npcs;
    }

    public List<Integer> listAllTalkedNpcs() {
        List<Integer> npcs = new ArrayList<Integer>();
        for (Pair<Integer, Integer> rawr : npcTalk.keySet()) {
            npcs.add(rawr.getRight());
        }
        return npcs;
    }

    public int talkedTimesByNpc(int npc) {
        int i = 0;
        for (Pair<Integer, Integer> rawr : npcTalk.keySet()) {
            if (rawr.getRight().equals(npc)) {
                i += npcTalk.get(rawr);
            }
        }
        return i;
    }
}