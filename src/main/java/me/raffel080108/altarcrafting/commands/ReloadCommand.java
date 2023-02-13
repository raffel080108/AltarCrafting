package me.raffel080108.altarcrafting.commands;

import me.raffel080108.altarcrafting.DataHandler;
import me.raffel080108.altarcrafting.utils.Utils;
import me.raffel080108.altarcrafting.AltarCrafting;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.CommandActor;

public final class ReloadCommand {
    private final AltarCrafting main;
    private final Utils utils;
    private final DataHandler dataHandler;

    public ReloadCommand(AltarCrafting main, DataHandler dataHandler) {
        this.main = main;
        this.dataHandler = dataHandler;
        this.utils = new Utils(main, dataHandler);
    }

    @Command("altarCrafting reload")
    @CommandPermission("altarCrafting.reload")
    private void reloadCommand(CommandActor sender) {
        main.saveDefaultConfig();
        if (utils.invalidConfigCheck()) {
            sender.reply("§aConfiguration reloaded successfully");
            main.getLogger().info("Configuration reloaded by " + sender.getName());
        } else sender.reply("§cError while reloading configuration, please check your console for detailed error-log");
        main.reloadConfig();
        dataHandler.setConfig(main.getConfig());

        utils.loadRecipes();
        utils.setupAutoComplete();
    }
}
