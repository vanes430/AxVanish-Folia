package com.artillexstudios.axvanish.command;

import com.artillexstudios.axapi.nms.wrapper.WrapperRegistry;
import com.artillexstudios.axapi.reflection.ClassUtils;
import com.artillexstudios.axapi.utils.MessageUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.utils.logging.LogUtils;
import com.artillexstudios.axvanish.AxVanishPlugin;
import com.artillexstudios.axvanish.api.AxVanishAPI;
import com.artillexstudios.axvanish.api.context.VanishContext;
import com.artillexstudios.axvanish.api.context.VanishSource;
import com.artillexstudios.axvanish.api.context.source.CommandVanishSource;
import com.artillexstudios.axvanish.api.context.source.ConsoleVanishSource;
import com.artillexstudios.axvanish.api.context.source.ForceVanishSource;
import com.artillexstudios.axvanish.api.context.source.ReloadVanishSource;
import com.artillexstudios.axvanish.api.users.User;
import com.artillexstudios.axvanish.config.Config;
import com.artillexstudios.axvanish.config.Groups;
import com.artillexstudios.axvanish.config.Language;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.setting.ManagerSetting;

import java.util.ArrayList;
import java.util.List;

public final class AxVanishCommand {

    private final AxVanishPlugin plugin;

    public AxVanishCommand(AxVanishPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        PaperCommandManager<CommandSourceStack> manager;
        try {
            manager = PaperCommandManager.builder()
                    .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                    .buildOnEnable(this.plugin);
            manager.settings().set(ManagerSetting.OVERRIDE_EXISTING_COMMANDS, true);
        } catch (Exception e) {
            LogUtils.error("Failed to initialize command manager", e);
            return;
        }

        manager.command(manager.commandBuilder("vanish", "v", "axvanish")
                .permission("axvanish.vanish")
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    if (!(sender instanceof Player player)) {
                        return;
                    }

                    User user = AxVanishAPI.instance().getUserIfLoadedImmediately(player);
                    if (user == null) {
                        MessageUtils.sendMessage(player, Language.prefix, Language.errorUserNotLoaded);
                        return;
                    }
                    if (Config.debug) {
                        LogUtils.debug("Debug is working!");
                    }

                    VanishContext vanishContext = new VanishContext.Builder()
                            .withSource(user)
                            .withSource(CommandVanishSource.INSTANCE)
                            .build();
                    boolean previous = user.vanished();
                    if (!user.update(!user.vanished(), vanishContext)) {
                        LogUtils.info("Failed to change state!");
                        // The user's visibility was not changed because an event was cancelled
                        return;
                    }

                    if (previous) {
                        MessageUtils.sendMessage(player, Language.prefix, Language.unvanishMessage);
                        broadcastVanishMessage(user, Language.unvanishBroadcast, Language.fakeJoin, Config.fakeJoin);
                    } else {
                        MessageUtils.sendMessage(player, Language.prefix, Language.vanishMessage);
                        broadcastVanishMessage(user, Language.vanishBroadcast, Language.fakeLeave, Config.fakeLeave);
                    }
                })
        );

        manager.command(manager.commandBuilder("vanish", "v", "axvanish")
                .literal("toggle")
                .permission("axvanish.command.toggle.other")
                .required("player", OfflinePlayerParser.offlinePlayerParser())
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    OfflinePlayer offlinePlayer = context.get("player");

                    VanishSource source = sender instanceof Player player ? AxVanishAPI.instance().getUserIfLoadedImmediately(player) : ConsoleVanishSource.INSTANCE;

                    AxVanishAPI.instance().user(offlinePlayer.getUniqueId()).thenAccept(user -> {
                        if (source instanceof User senderUser) {
                            if (senderUser.group() != null && user.group() != null && senderUser.group().priority() < user.group().priority()) {
                                MessageUtils.sendMessage(sender, Language.prefix, Language.errorNotHighEnoughPriority);
                                return;
                            }
                        }

                        VanishContext vanishContext = new VanishContext.Builder()
                                .withSource(source)
                                .withSource(CommandVanishSource.INSTANCE)
                                .build();

                        boolean previous = user.vanished();
                        if (!user.update(!user.vanished(), vanishContext)) {
                            LogUtils.info("Failed to change state!");
                            return;
                        }

                        if (previous) {
                            MessageUtils.sendMessage(sender, Language.prefix, Language.unvanishMessage);
                            broadcastVanishMessage(user, Language.unvanishBroadcast, Language.fakeJoin, Config.fakeJoin);
                        } else {
                            MessageUtils.sendMessage(sender, Language.prefix, Language.vanishMessage);
                            broadcastVanishMessage(user, Language.vanishBroadcast, Language.fakeLeave, Config.fakeLeave);
                        }
                    });
                })
        );

        manager.command(manager.commandBuilder("vanish", "v", "axvanish")
                .literal("admin")
                .permission("axvanish.command.admin")
                .literal("version")
                .permission("axvanish.command.admin.version")
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    MessageUtils.sendMessage(sender, Language.prefix, "<green>You are running <white>AxVanish</white> version <white><version></white> on <white><implementation></white> version <white><implementation-version></white> (Implementing API version <white><api-version></white>)",
                            Placeholder.unparsed("version", this.plugin.getDescription().getVersion()),
                            Placeholder.unparsed("implementation", Bukkit.getName()),
                            Placeholder.unparsed("implementation-version", Bukkit.getVersion()),
                            Placeholder.unparsed("api-version", Bukkit.getBukkitVersion())
                    );
                })
        );

        manager.command(manager.commandBuilder("vanish", "v", "axvanish")
                .literal("admin")
                .permission("axvanish.command.admin")
                .literal("reload")
                .permission("axvanish.command.admin.reload")
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    long start = System.nanoTime();
                    List<String> failed = new ArrayList<>();

                    if (!Config.reload()) {
                        failed.add("config.yml");
                    }

                    if (!Language.reload()) {
                        failed.add("language/" + Language.lastLanguage + ".yml");
                    }

                    if (!Groups.reload()) {
                        failed.add("groups.yml");
                    }

                    for (User user : AxVanishAPI.instance().online()) {
                        if (user.vanished()) {
                            user.update(false, new VanishContext.Builder()
                                    .withSource(ReloadVanishSource.INSTANCE)
                                    .withSource(ForceVanishSource.INSTANCE)
                                    .build()
                            );
                        }
                    }

                    if (failed.isEmpty()) {
                        MessageUtils.sendMessage(sender, Language.prefix, Language.reloadSuccess, Placeholder.unparsed("time", Long.toString((System.nanoTime() - start) / 1_000_000)));
                    } else {
                        MessageUtils.sendMessage(sender, Language.prefix, Language.reloadFail, Placeholder.unparsed("time", Long.toString((System.nanoTime() - start) / 1_000_000)), Placeholder.unparsed("files", String.join(", ", failed)));
                    }
                })
        );
    }

    private void broadcastVanishMessage(User user, String broadcastMessage, String fakeMessage, boolean fakeEnabled) {
        String name = user.player().getName() != null ? user.player().getName() : "Unknown";

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Send staff notification (excluding self to avoid double messages for the vanished player)
            if (player.hasPermission("axvanish.notify") && !player.getUniqueId().equals(user.player().getUniqueId())) {
                MessageUtils.sendMessage(player, Language.prefix, broadcastMessage, Placeholder.unparsed("player", name));
            }

            // Send fake join/leave message to EVERYONE (including self)
            if (fakeEnabled) {
                String message = fakeMessage;
                if (ClassUtils.INSTANCE.classExists("me.clip.placeholderapi.PlaceholderAPI")) {
                    message = PlaceholderAPI.setPlaceholders(user.onlinePlayer() == null ? user.player() : user.onlinePlayer(), message);
                }
                Component formatted = StringUtils.format(message, Placeholder.unparsed("player", name));
                WrapperRegistry.SERVER_PLAYER.map(player).message(formatted);
            }
        }
    }
}
