package net.sf.odinms.net.channel;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.messages.CommandProcessor;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.MapleServerHandler;
import net.sf.odinms.net.PacketProcessor;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.mina.MapleCodecFactory;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.guild.MapleGuild;
import net.sf.odinms.net.world.guild.MapleGuildCharacter;
import net.sf.odinms.net.world.guild.MapleGuildSummary;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.net.world.remote.WorldRegistry;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.scripting.event.EventScriptManager;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.ClanHolder;
import net.sf.odinms.server.MapleSquad;
import net.sf.odinms.server.MapleSquadType;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.PlayerInteraction.HiredMerchant;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.maps.FakeCharacter;
import net.sf.odinms.server.maps.MapleMapFactory;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

public class ChannelServer implements Runnable, ChannelServerMBean {

    private static int uniqueID = 1;
    private int port = 7575;
    private static Properties initialProp;
    private static WorldRegistry worldRegistry;
    private PlayerStorage players = new PlayerStorage();
    private String serverMessage;
    private int expRate;
    private int mesoRate;
    private int dropRate;
    private int bossdropRate;
    private int petExpRate;
    private boolean dropUndroppables;
    private boolean moreThanOne;
    private int channel;
    private String key;
    private Properties props = new Properties();
    private ChannelWorldInterface cwi;
    private WorldChannelInterface wci = null;
    private IoAcceptor acceptor;
    private String ip;
    private boolean shutdown = false;
    private boolean finishedShutdown = false;
    private String arrayString = "";
    private String serverName;
    private boolean AB;
    private boolean godlyItems;
    private boolean CS;
    private boolean MT;
    private boolean GMItems;
    private boolean extraCommands;
    private short itemStatMultiplier;
    private short godlyItemRate;
    private int PvPis;
    private MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private static Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
    private static Map<String, ChannelServer> pendingInstances = new HashMap<String, ChannelServer>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<Integer, MapleGuildSummary>();
    private Boolean worldReady = true;
    private Map<MapleSquadType, MapleSquad> mapleSquads = new HashMap<MapleSquadType, MapleSquad>();
    private ClanHolder clans = new ClanHolder();
    private Collection<FakeCharacter> clones = new LinkedList<FakeCharacter>();
    private int levelCap;
    private boolean multiLevel;

    private ChannelServer(String key) {
        mapFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz")));
        this.key = key;
    }

    public static WorldRegistry getWorldRegistry() {
        return worldRegistry;
    }

    public void reconnectWorld() {
        // Check if the connection is really gone.
        try {
            wci.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = false;
            }
            synchronized (cwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server.");
                synchronized (wci) {
                    // Completely re-establish the rmi connection.
                    try {
                        initialProp = new Properties();
                        FileReader fr = new FileReader(System.getProperty("net.sf.odinms.channel.config"));
                        initialProp.load(fr);
                        fr.close();
                        Registry registry = LocateRegistry.getRegistry(initialProp.getProperty("net.sf.odinms.world.host"),
                        Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
                        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
                        cwi = new ChannelWorldInterfaceImpl(this);
                        wci = worldRegistry.registerChannelServer(key, cwi);
                        props = wci.getGameProperties();
                        expRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.exp"));
                        mesoRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.meso"));
                        dropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.drop"));
                        bossdropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.bossdrop"));
                        petExpRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.petExp"));
                        serverMessage = props.getProperty("net.sf.odinms.world.serverMessage");
                        dropUndroppables = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.alldrop", "false"));
                        moreThanOne = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.morethanone", "false"));
                        serverName = props.getProperty("net.sf.odinms.world.serverName");
                        godlyItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.godlyItems", "false"));
                        itemStatMultiplier = Short.parseShort(props.getProperty("net.sf.odinms.world.itemStatMultiplier"));
                        godlyItemRate = Short.parseShort(props.getProperty("net.sf.odinms.world.godlyItemRate"));
                        multiLevel = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.multiLevel", "false"));
                        levelCap = Integer.parseInt(props.getProperty("net.sf.odinms.world.levelCap"));
                        AB = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.Autoban", "false"));
                        CS = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.CashShop", "false"));
                        MT = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.MTS", "false"));
                        GMItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.GMItems", "false"));
                        extraCommands = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.extraCommands", "false"));
                        PvPis = Integer.parseInt(props.getProperty("net.sf.odinms.world.PvPis", "4"));
                        Properties dbProp = new Properties();
                        fr = new FileReader("db.properties");
                        dbProp.load(fr);
                        fr.close();
                        DatabaseConnection.setProps(dbProp);
                        DatabaseConnection.getConnection();
                        wci.serverReady();
                    } catch (Exception e) {
                        System.out.println("Reconnecting failed" + e);
                    }
                    worldReady = true;
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        try {
            cwi = new ChannelWorldInterfaceImpl(this);
            wci = worldRegistry.registerChannelServer(key, cwi);
            props = wci.getGameProperties();
            expRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.exp"));
            mesoRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.meso"));
            dropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.drop"));
            bossdropRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.bossdrop"));
            petExpRate = Integer.parseInt(props.getProperty("net.sf.odinms.world.petExp"));
            serverMessage = props.getProperty("net.sf.odinms.world.serverMessage");
            dropUndroppables = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.alldrop", "false"));
            moreThanOne = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.morethanone", "false"));
            eventSM = new EventScriptManager(this, props.getProperty("net.sf.odinms.channel.events").split(","));
            serverName = props.getProperty("net.sf.odinms.world.serverName");
            godlyItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.godlyItems", "false"));
            itemStatMultiplier = Short.parseShort(props.getProperty("net.sf.odinms.world.itemStatMultiplier"));
            godlyItemRate = Short.parseShort(props.getProperty("net.sf.odinms.world.godlyItemRate"));
            multiLevel = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.multiLevel", "false"));
            levelCap = Integer.parseInt(props.getProperty("net.sf.odinms.world.levelCap"));
            AB = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.Autoban", "false"));
            CS = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.CashShop", "false"));
            MT = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.MTS", "false"));
            GMItems = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.GMItems", "false"));
            extraCommands = Boolean.parseBoolean(props.getProperty("net.sf.odinms.world.extraCommands", "false"));
            PvPis = Integer.parseInt(props.getProperty("net.sf.odinms.world.PvPis", "4"));
            Properties dbProp = new Properties();
            FileReader fileReader = new FileReader("db.properties");
            dbProp.load(fileReader);
            fileReader.close();
            DatabaseConnection.setProps(dbProp);
            DatabaseConnection.getConnection();
            Connection c = DatabaseConnection.getConnection();
            PreparedStatement ps;
            try {
                ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
                ps.executeUpdate();
                ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
                System.out.println("Could not reset databases" + ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        port = Integer.parseInt(props.getProperty("net.sf.odinms.channel.net.port"));
        ip = props.getProperty("net.sf.odinms.channel.net.interface") + ":" + port;
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        acceptor = new SocketAcceptor();
        SocketAcceptorConfig cfg = new SocketAcceptorConfig();
        // cfg.setThreadModel(ThreadModel.MANUAL); // *fingers crossed*, I hope the executor filter handles everything
        // executor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        // cfg.getFilterChain().addLast("executor", new ExecutorFilter(executor));
        cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        // Item.loadInitialDataFromDB();
        TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        tMan.register(AutobanManager.getInstance(), 60000);
        try {
            MapleServerHandler serverHandler = new MapleServerHandler(PacketProcessor.getProcessor(PacketProcessor.Mode.CHANNELSERVER), channel);
            acceptor.bind(new InetSocketAddress(port), serverHandler, cfg);
            System.out.println("Channel " + getChannel() + ": Listening on port: " + port);
            wci.serverReady();
            eventSM.init();
        } catch (IOException e) {
            System.out.println("Binding to port " + port + " failed (ch: " + getChannel() + ")" + e);
        }
    }

    public void shutdown() {
        shutdown = true;
        List<CloseFuture> futures = new LinkedList<CloseFuture>();
        Collection<MapleCharacter> allchars = players.getAllCharacters();
        MapleCharacter chrs[] = allchars.toArray(new MapleCharacter[allchars.size()]);
        for (MapleCharacter chr : chrs) {
            if (chr.getTrade() != null) {
                MapleTrade.cancelTrade(chr);
            }
            if (chr.getEventInstance() != null) {
                chr.getEventInstance().playerDisconnected(chr);
            }
            if (!chr.getClient().isGuest()) {
                chr.saveToDB(true, true);
            }
            if (chr.getCheatTracker() != null) {
                chr.getCheatTracker().dispose();
            }
            removePlayer(chr);
        }
        for (MapleCharacter chr : chrs) {
            futures.add(chr.getClient().getSession().close());
        }
        for (CloseFuture future : futures) {
            future.join(500);
        }
        finishedShutdown = true;
        wci = null;
        cwi = null;
    }

    public void unbind() {
        acceptor.unbindAll();
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public static ChannelServer newInstance(String key) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        ChannelServer instance = new ChannelServer(key);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(instance, new ObjectName("net.sf.odinms.net.channel:type=ChannelServer,name=ChannelServer" + uniqueID++));
        pendingInstances.put(key, instance);
        return instance;
    }

    public static ChannelServer getInstance(int channel) {
        return instances.get(channel);
    }

    public void addPlayer(MapleCharacter chr) {
        players.registerPlayer(chr);
        if (chr.getClan() > -1) {
            clans.playerOnline(chr);
        }
    }

    public IPlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.deregisterPlayer(chr);
        if (chr.getClan() > -1) {
            clans.deregisterPlayer(chr);
        }
    }

    public void addToClan(MapleCharacter chr) {
        clans.registerPlayer(chr);
    }

    public ClanHolder getClanHolder() {
        return clans;
    }

    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    @Override
    public String getServerMessage() {
        return serverMessage;
    }

    @Override
    public void setServerMessage(String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public void broadcastPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            chr.getClient().getSession().write(data);
        }
    }

    @Override
    public int getExpRate() {
        return expRate;
    }

    @Override
    public void setExpRate(int expRate) {
        this.expRate = expRate;
    }

    public String getArrayString() {
        return arrayString;
    }

    public void setArrayString(String newStr) {
        arrayString = newStr;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        if (pendingInstances.containsKey(key)) {
            pendingInstances.remove(key);
        }
        if (instances.containsKey(channel)) {
            instances.remove(channel);
        }
        instances.put(channel, this);
        this.channel = channel;
        this.mapFactory.setChannel(channel);
    }

    public static Collection<ChannelServer> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    public String getIP() {
        return ip;
    }

    public String getIP(int channel) {
        try {
            return getWorldInterface().getIP(channel);
        } catch (RemoteException e) {
            System.out.println("Lost connection to world server" + e);
            throw new RuntimeException("Lost connection to world server");
        }
    }

    public WorldChannelInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return wci;
    }

    public String getProperty(String name) {
        return props.getProperty(name);
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void shutdown(int time) {
        broadcastPacket(MaplePacketCreator.serverNotice(0, "The world will be shut down in " + (time / 60000) + " minutes, please log off safely"));
        TimerManager.getInstance().schedule(new ShutdownServer(getChannel()), time);
    }

    @Override
    public void shutdownWorld(int time) {
        try {
            getWorldInterface().shutdown(time);
        } catch (RemoteException e) {
            reconnectWorld();
        }
    }

    public int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, props.getProperty("net.sf.odinms.channel.events").split(","));
        eventSM.init();
    }

    @Override
    public int getMesoRate() {
        return mesoRate;
    }

    @Override
    public void setMesoRate(int mesoRate) {
        this.mesoRate = mesoRate;
    }

    @Override
    public int getDropRate() {
        return dropRate;
    }

    @Override
    public void setDropRate(int dropRate) {
        this.dropRate = dropRate;
    }

    @Override
    public int getBossDropRate() {
        return bossdropRate;
    }

    @Override
    public void setBossDropRate(int bossdropRate) {
        this.bossdropRate = bossdropRate;
    }

    @Override
    public int getPetExpRate() {
        return petExpRate;
    }

    @Override
    public void setPetExpRate(int petExpRate) {
        this.petExpRate = petExpRate;
    }

    public boolean allowUndroppablesDrop() {
        return dropUndroppables;
    }

    public boolean allowMoreThanOne() {
        return moreThanOne;
    }

    public MapleGuild getGuild(MapleGuildCharacter mgc) {
        int gid = mgc.getGuildId();
        MapleGuild g = null;
        try {
            g = this.getWorldInterface().getGuild(gid, mgc);
        } catch (RemoteException re) {
            System.out.println("RemoteException while fetching MapleGuild." + re);
            return null;
        }

        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }

        return g;
    }

    public MapleGuildSummary getGuildSummary(int gid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            try {
                MapleGuild g = this.getWorldInterface().getGuild(gid, null);
                if (g != null) {
                    gsStore.put(gid, new MapleGuildSummary(g));
                }
                return gsStore.get(gid);
            } catch (RemoteException re) {
                System.out.println("RemoteException while fetching GuildSummary." + re);
                return null;
            }
        }
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        try {
            MapleGuild g;
            for (int i : gsStore.keySet()) {
                g = this.getWorldInterface().getGuild(i, null);
                if (g != null) {
                    gsStore.put(i, new MapleGuildSummary(g));
                } else {
                    gsStore.remove(i);
                }
            }
        } catch (RemoteException re) {
            System.out.println("RemoteException while reloading GuildSummary." + re);
        }
    }

    public static void main(String args[]) throws FileNotFoundException, IOException, NotBoundException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        initialProp = new Properties();
        initialProp.load(new FileReader(System.getProperty("net.sf.odinms.channel.config")));
        Registry registry = LocateRegistry.getRegistry(initialProp.getProperty("net.sf.odinms.world.host"), Registry.REGISTRY_PORT, new SslRMIClientSocketFactory());
        worldRegistry = (WorldRegistry) registry.lookup("WorldRegistry");
        for (int i = 0; i < Integer.parseInt(initialProp.getProperty("net.sf.odinms.channel.count", "0")); i++) {
            newInstance(initialProp.getProperty("net.sf.odinms.channel." + i + ".key")).run();
        }
        DatabaseConnection.getConnection();
        CommandProcessor.registerMBean();
        ClanHolder.loadAllClans();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (ChannelServer channel : getAllInstances()) {
                    for (int i = 910000001; i <= 910000022; i++) {
                        for (MapleMapObject obj : channel.getMapFactory().getMap(i).getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT))) {
                            HiredMerchant hm = (HiredMerchant) obj;
                            hm.closeShop(true);
                        }
                    }
                    for (MapleCharacter mc : channel.getPlayerStorage().getAllCharacters()) {
                        mc.saveToDB(true, true);
                    }
                }
            }
        });
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public boolean addMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.get(type) == null) {
            mapleSquads.remove(type);
            mapleSquads.put(type, squad);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMapleSquad(MapleSquad squad, MapleSquadType type) {
        if (mapleSquads.containsKey(type)) {
            if (mapleSquads.get(type) == squad) {
                mapleSquads.remove(type);
                return true;
            }
        }
        return false;
    }

    public boolean isGodlyItems() {
        return godlyItems;
    }

    public void setGodlyItems(boolean blahblah) {
        this.godlyItems = blahblah;
    }

    public short getItemMultiplier() {
        return itemStatMultiplier;
    }

    public void setItemMultiplier(Short blahblah) {
        this.itemStatMultiplier = blahblah;
    }

    public short getGodlyItemRate() {
        return godlyItemRate;
    }

    public void setGodlyItemRate(Short blahblah) {
        this.godlyItemRate = blahblah;
    }

    public void broadcastSMega(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.getSmegaEnabled()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public void broadcastGMPacket(MaplePacket data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.getClient().getSession().write(data);
            }
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void broadcastToClan(MaplePacket data, int clan) {
        for (MapleCharacter chr : clans.getAllOnlinePlayersFromClan(clan)) {
            chr.getClient().getSession().write(data);
        }
    }

    public int onlineClanMembers(int clan) {
        return clans.countOnlineByClan(clan);
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new LinkedList<MapleCharacter>();
        for (net.sf.odinms.net.world.MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getChannel()) { // Make sure the thing doesn't get duplicate plays due to ccing bug.
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public void addClone(FakeCharacter fc) {
        clones.add(fc);
    }

    public void removeClone(FakeCharacter fc) {
        clones.remove(fc);
    }

    public Collection<FakeCharacter> getAllClones() {
        return clones;
    }

    public int getLevelCap() {
        return levelCap;
    }

    public boolean getMultiLevel() {
        return multiLevel;
    }

    public boolean AutoBan() {
        return AB;
    }

    public boolean CStoFM() {
        return CS;
    }

    public boolean MTtoFM() {
        return MT;
    }

    public boolean CanGMItem() {
        return GMItems;
    }
    
    public boolean extraCommands() {
        return extraCommands;
    }

    public int PvPis() {
        if (PvPis > 0 && PvPis < 21 || PvPis >= 100000000 && PvPis <=990000000) {
            return PvPis;
        } else {
            return 4;
        }
    }
}