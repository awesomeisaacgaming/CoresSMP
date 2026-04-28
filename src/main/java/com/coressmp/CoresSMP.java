package com.coressmp;

import com.coressmp.command.*;
import com.coressmp.listener.*;
import com.coressmp.manager.CoreManager;
import com.coressmp.manager.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;

public class CoresSMP extends JavaPlugin {

    private static CoresSMP instance;
    private CoreManager coreManager;
    private CooldownManager cooldownManager;
    private FireCoreListener fireListener;
    private WindCoreListener windListener;
    private EnderCoreListener enderListener;
    private IceCoreListener iceListener;
    private OceanCoreListener oceanListener;
    private EchoCoreListener echoListener;
    private WealthCoreListener wealthListener;

    @Override
    public void onEnable() {
        instance = this;
        coreManager = new CoreManager(this);
        cooldownManager = new CooldownManager();

        registerListeners();
        registerCommands();
        new PassiveTicker(this).start();

        getLogger().info("CoresSMP enabled!");
    }

    @Override
    public void onDisable() {
        if (coreManager != null) {
            coreManager.saveAll();
        }
        getLogger().info("CoresSMP disabled!");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new InventoryListener(this), this);
        fireListener = new FireCoreListener(this);
        windListener = new WindCoreListener(this);
        enderListener = new EnderCoreListener(this);
        iceListener = new IceCoreListener(this);
        oceanListener = new OceanCoreListener(this);
        echoListener = new EchoCoreListener(this);
        wealthListener = new WealthCoreListener(this);
        pm.registerEvents(fireListener, this);
        pm.registerEvents(windListener, this);
        pm.registerEvents(enderListener, this);
        pm.registerEvents(iceListener, this);
        pm.registerEvents(oceanListener, this);
        pm.registerEvents(echoListener, this);
        pm.registerEvents(wealthListener, this);
        pm.registerEvents(new SculkSuppressListener(this), this);
        pm.registerEvents(new WealthTotemListener(this), this);
    }

    private void registerCommands() {
        getCommand("ability1").setExecutor(new AbilityCommand(this, 1));
        getCommand("ability2").setExecutor(new AbilityCommand(this, 2));
        getCommand("ice").setExecutor(new IceToggleCommand(this));
        GiveCoreCommand giveCoreCmd = new GiveCoreCommand(this);
        PluginCommand giveCore = getCommand("givecore");
        giveCore.setExecutor(giveCoreCmd);
        giveCore.setTabCompleter(giveCoreCmd);
    }

    public static CoresSMP getInstance() { return instance; }
    public CoreManager getCoreManager() { return coreManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public FireCoreListener getFireListener() { return fireListener; }
    public WindCoreListener getWindListener() { return windListener; }
    public EnderCoreListener getEnderListener() { return enderListener; }
    public IceCoreListener getIceListener() { return iceListener; }
    public OceanCoreListener getOceanListener() { return oceanListener; }
    public EchoCoreListener getEchoListener() { return echoListener; }
    public WealthCoreListener getWealthListener() { return wealthListener; }
}
