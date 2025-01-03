package net.villagerzock.neocraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.villagerzock.neocraft.Teams.*;
import net.villagerzock.neocraft.config.Config;
import org.joml.Vector2i;

import java.util.Optional;

public class Commands {
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        System.out.println("Registering Commands");
        dispatcher.register(CommandManager.literal("neocraft")
                .then(CommandManager.literal("team")
                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("team_name", StringArgumentType.word()).executes(Commands::createTeam)
                                )
                ).then(
                                ConfigureHandler.build(dispatcher,commandRegistryAccess,registrationEnvironment)
                        ).then(
                                CommandManager.literal("claim").executes(Commands::claim)
                                        .then(CommandManager.argument("team",StringArgumentType.word()).requires(serverCommandSource -> {
                                            return serverCommandSource.hasPermissionLevel(2);
                                        }).executes((commandContext -> {
                                            ChunkPos pos = commandContext.getSource().getPlayer().getChunkPos();
                                            Vector2i chunk = new Vector2i(pos.x,pos.z);

                                            String team = commandContext.getArgument("team",String.class);

                                            ChunkManager.claim(chunk,team);
                                            return 0;
                                        }))
                                )
                        ).then(
                                CommandManager.literal("get")
                                        .then(CommandManager.argument("player",EntityArgumentType.player()).requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(commandContext -> {
                                            EntitySelector selector = commandContext.getArgument("player",EntitySelector.class);
                                            ServerPlayerEntity player = selector.getPlayer(commandContext.getSource());
                                            if (player instanceof PlayerAccessor accessor){
                                                if (accessor.isInATeam()){
                                                    commandContext.getSource().sendMessage(player.getDisplayName().copy().append(Text.literal(" §2ist im Team ").append(TeamManager.TEAMS.get(accessor.getTeam()).getDisplayName())));
                                                }else {
                                                    commandContext.getSource().sendMessage(player.getDisplayName().copy().append(Text.literal(" §2ist in keinem Team")));
                                                }
                                            }
                                            return 0;
                                        }))
                                        .executes(commandContext -> {
                                    ServerPlayerEntity player = commandContext.getSource().getPlayer();
                                    if (player instanceof PlayerAccessor){
                                        PlayerAccessor accessor = (PlayerAccessor) player;
                                        if (accessor.isInATeam()){
                                            commandContext.getSource().sendMessage(Text.literal("§2Du bist im Team ").append(TeamManager.TEAMS.get(accessor.getTeam()).getDisplayName()));
                                        }else {
                                            commandContext.getSource().sendMessage(Text.literal("§2Du bist in keinem Team"));
                                        }

                                    }
                                    return 0;
                                })
                        ).then(
                                CommandManager.literal("invite").then(
                                        CommandManager.argument("player",EntityArgumentType.player()).executes(Commands::invitePlayer)
                                )
                        ).then(
                                CommandManager.literal("accept").then(
                                        CommandManager.argument("player",EntityArgumentType.player()).executes(Commands::acceptInvite)
                                )
                        ).then(
                                CommandManager.literal("leave").executes(Commands::leave)
                        )
                )

        );
        dispatcher.register(CommandManager.literal("tablist")
                .requires(serverCommandSource -> {
                    return serverCommandSource.hasPermissionLevel(4);
                })
                .then(CommandManager.literal("header")
                        .then(CommandManager.argument("text",TextArgumentType.text(commandRegistryAccess)).executes(Commands::tablistHeader))
                )
                .then(CommandManager.literal("footer")
                        .then(CommandManager.argument("text",TextArgumentType.text(commandRegistryAccess)).executes(Commands::tablistFooter))
                )
        );
    }

    public static int leave(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player instanceof PlayerAccessor accessor){
            accessor.setTeam("");
            player.sendMessage(Text.literal("§2du hast dein Team jetzt verlassen"));
        }
        return 0;
    }

    public static int tablistFooter(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        return 0;
    }

    public static int tablistHeader(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        return 0;
    }

    public static int acceptInvite(CommandContext<ServerCommandSource> serverCommandSourceCommandContext){
        EntitySelector selector = serverCommandSourceCommandContext.getArgument("player",EntitySelector.class);
        ServerPlayerEntity player = null;
        try {
            player = selector.getPlayer(serverCommandSourceCommandContext.getSource());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        ServerPlayerEntity sender = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player instanceof PlayerAccessor accessor){
            if (sender instanceof PlayerAccessor senderAccessor){
                if (TeamManager.TEAMS.containsKey(accessor.getTeam())){
                    TeamManager manager = TeamManager.TEAMS.get(accessor.getTeam());
                    if (manager.invites.contains(sender.getName().getString())){
                        manager.invites.remove(sender.getName().getString());
                        senderAccessor.setTeam(accessor.getTeam());
                        serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Du bist jetzt im Team " + senderAccessor.getTeam()));
                    }
                }
            }
        }
        return 0;
    }

    public static int invitePlayer(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        EntitySelector selector = serverCommandSourceCommandContext.getArgument("player",EntitySelector.class);
        ServerPlayerEntity player = null;
        try {
            player = selector.getPlayer(serverCommandSourceCommandContext.getSource());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        ServerPlayerEntity sender = serverCommandSourceCommandContext.getSource().getPlayer();
        if (sender instanceof PlayerAccessor senderAccessor){
            if (player instanceof PlayerAccessor accessor){
                if (TeamManager.TEAMS.containsKey(senderAccessor.getTeam())){
                    if (accessor.getTeam() != senderAccessor.getTeam()){
                        player.sendMessage(sender.getDisplayName().copy().append(Text.literal("§2 Hat dir eine Team Einladung gesendet")));
                        TeamManager manager = TeamManager.TEAMS.get(senderAccessor.getTeam());
                        manager.invites.add(player.getName().getString());
                        Text join = Text.literal("§3[Beitreten]").fillStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/neocraft team accept " + sender.getName().getString())));
                        if (TeamManager.TEAMS.containsKey(accessor.getTeam())){
                            player.sendMessage(Text.literal("§2Doch da du in einem Team bist muss du dieses zuerst verlassen "));
                        }else {
                            player.sendMessage(Text.literal("§2Willst du diesem beitreten? ").append(join));
                        }
                        sender.sendMessage(Text.literal("§2Du hast ").append(player.getDisplayName()).append("§2 erfogreich eingeladen"));
                    }else {
                        serverCommandSourceCommandContext.getSource().sendError(Text.literal("Dieser Spieler ist bereits in deinem Team"));
                    }
                }else {
                    serverCommandSourceCommandContext.getSource().sendError(Text.literal("Du musst in einem Team sein"));
                }
            }
        }
        return 0;
    }
    public static int getRemainingClaims(PlayerEntity player){
        if (player instanceof PlayerAccessor accessor){
            int alreadyClaimedChunks = ChunkManager.getAmountOfClaimedChunks(player);
            int maxClaims = accessor.getTeamManager().maxClaims;
            int mul = Config.CLAIMS_PER_PERSON.get();
            return (maxClaims * mul) - alreadyClaimedChunks;
        }
        return 0;
    }

    public static int claim(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        try {
            ChunkPos pos = serverCommandSourceCommandContext.getSource().getPlayer().getChunkPos();
            Vector2i chunk = new Vector2i(pos.x,pos.z);
            ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();

            switch (ChunkManager.claim(chunk,player)){
                case NOT_ENOUGHT_CLAIMS:
                    serverCommandSourceCommandContext.getSource().sendError(Text.literal("Dein Team hat keine Claims mehr übrig"));
                    break;
                case SUCCESS:
                    serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Dieser Chunk gehört nun dir, " + getRemainingClaims(player) + " verbleibend"));
                    break;
                case ALREADY_CLAIMED:
                    break;
                case ALREADY_YOURS:
                    break;
            }
            return 0;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int changeDisplayName(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player instanceof PlayerAccessor accessor){
            String team = accessor.getTeam();
            System.out.println(team);
            if (!TeamManager.TEAMS.containsKey(team)){
                serverCommandSourceCommandContext.getSource().sendError(Text.literal("Du musst in einem Team sein um diesen befehl zu nutzen"));
                return 0;
            }
            TeamManager manager = TeamManager.TEAMS.get(team);
            Text text = serverCommandSourceCommandContext.getArgument("name", Text.class);
            manager.changeDisplayname(text);
            serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2der Displayname wurde zu §r").append(text).append(Text.literal("§2 geändert")));
        }
        return 0;
    }

    public static int createTeam(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        String teamName = serverCommandSourceCommandContext.getArgument("team_name",String.class);
        Optional<TeamManager> manager = TeamManager.create(teamName,serverCommandSourceCommandContext.getSource().getPlayer().getUuid());
        if (manager.isPresent()){
            ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();
            if (player instanceof PlayerAccessor){
                PlayerAccessor accessor = (PlayerAccessor) player;
                accessor.setTeam(teamName);
            }
            serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Team erstellt"));
            serverCommandSourceCommandContext.getSource().getPlayer().playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:block.note_block.bell")), SoundCategory.PLAYERS,100,0);
            return 0;
        }else {
            serverCommandSourceCommandContext.getSource().sendError(Text.literal("Dieses Team existiert bereits"));
            serverCommandSourceCommandContext.getSource().getPlayer().playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:block.note_block.didgeridoo")), SoundCategory.PLAYERS,100,0);
            return 1;
        }
    }

    public static int friendlyFire(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        serverCommandSource.sendMessage(Text.literal("Dies ist eine Funktion welche zum testen ist, und hat keinen Speziellen Einfluss"));
        if (serverCommandSource.getPlayer() instanceof PlayerAccessor accessor){
            if (accessor.isInATeam()){
                TeamManager manager = accessor.getTeamManager();
                manager.getSettings().friendlyFire = aBoolean;
            }
        }
        return 0;
    }

    public static int attack_entities(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        if (serverCommandSource.getPlayer() instanceof  PlayerAccessor accessor){
            accessor.getTeamManager().getSettings().attack_entities = aBoolean;
        }
        return 0;
    }

    public static int interact_entities(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        if (serverCommandSource.getPlayer() instanceof  PlayerAccessor accessor){
            accessor.getTeamManager().getSettings().interact_entities = aBoolean;
        }
        return 0;
    }
}
