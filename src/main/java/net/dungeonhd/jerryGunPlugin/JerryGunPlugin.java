package net.dungeonhd.jerryGunPlugin;

import net.dungeonhd.jerryGunPlugin.ability.JerrychineGunListener;
import net.dungeonhd.jerryGunPlugin.command.GiveItemCommand;
import net.dungeonhd.jerryGunPlugin.item.JerrychineGunItem;
import net.dungeonhd.jerryGunPlugin.mana.ManaManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JerryGunPlugin extends JavaPlugin {

    private JerrychineGunItem gunItem;
    private ManaManager manaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.gunItem = new JerrychineGunItem(this);
        this.manaManager = new ManaManager(this);
        manaManager.start();

        getServer().getPluginManager().registerEvents(new JerrychineGunListener(this), this);

        GiveItemCommand giveItemCommand = new GiveItemCommand(this);
        getCommand("giveitem").setExecutor(giveItemCommand);
        getCommand("giveitem").setTabCompleter(giveItemCommand);

        getLogger().info("Jerry-chine Gun loaded");
    }

    @Override
    public void onDisable() {
        if (manaManager != null) {
            manaManager.stop();
        }
    }

    public JerrychineGunItem getGunItem() {
        return gunItem;
    }

    public ManaManager getManaManager() {
        return manaManager;
    }
}
