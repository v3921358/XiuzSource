var setupTask;

function init() {
    scheduleNew();
}

function scheduleNew() {
	setupTask = em.scheduleAtFixedRate("start", 1000 * 60 * 10);
}

function cancelSchedule() {
    setupTask.cancel(true);
}

function start() {
    var Message = new Array("@slime Will sell you slimes for 50mil for our reward system.","Vote every 12 hours so we can get more players,","@commands Will show a list of commands, Hector bot in fm will also help you!");
    em.getChannelServer().broadcastPacket(net.sf.odinms.tools.MaplePacketCreator.sendYellowTip("[XiuzSource] " + Message[Math.floor(Math.random() * Message.length)]));
}