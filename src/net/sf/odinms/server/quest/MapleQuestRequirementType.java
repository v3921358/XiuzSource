package net.sf.odinms.server.quest;

public enum MapleQuestRequirementType {
    UNDEFINED(-1), JOB(0), ITEM(1), QUEST(2), MIN_LEVEL(3), MAX_LEVEL(4), END_DATE(5), MOB(6), NPC(7), FIELD_ENTER(8), INTERVAL(9), SCRIPT(10), PET(11), MIN_PET_TAMENESS(12);

    public MapleQuestRequirementType getITEM() {
        return ITEM;
    }

    final byte type;

    private MapleQuestRequirementType(int type) {
        this.type = (byte)type;
    }

    public byte getType() {
        return type;
    }

    public static MapleQuestRequirementType getByType(byte type) {
        for (MapleQuestRequirementType l : MapleQuestRequirementType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public static MapleQuestRequirementType getByWZName(String name) {
        if (name.equals("job")) return JOB;
        else if (name.equals("quest")) return QUEST;
        else if (name.equals("item")) return ITEM;
        else if (name.equals("lvmin")) return MIN_LEVEL;
        else if (name.equals("lvmax")) return MAX_LEVEL;
        else if (name.equals("end")) return END_DATE;
        else if (name.equals("mob")) return MOB;
        else if (name.equals("npc")) return NPC;
        else if (name.equals("fieldEnter")) return FIELD_ENTER;
        else if (name.equals("interval")) return INTERVAL;
        else if (name.equals("startscript")) return SCRIPT;
        else if (name.equals("endscript")) return SCRIPT;
        else if (name.equals("pet")) return PET;
        else if (name.equals("pettamenessmin")) return MIN_PET_TAMENESS;
        else return UNDEFINED;
    }
}