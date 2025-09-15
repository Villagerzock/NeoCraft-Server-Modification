package net.villagerzock.neocraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.ColumnPosArgumentType;
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
import net.minecraft.util.math.ColumnPos;
import net.villagerzock.neocraft.Teams.*;
import net.villagerzock.neocraft.config.Config;
import org.joml.Vector2i;

import java.util.Optional;

import static net.villagerzock.neocraft.Neocraft.logger;

public class Commands {
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        System.out.println("Registering Commands");
        dispatcher.register(CommandManager.literal(Config.COMMAND_NAME.get())
                .then(CommandManager.literal("team")
                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("team_name", StringArgumentType.word()).executes(Commands::createTeam)
                                )
                ).then(
                                ConfigureHandler.build(dispatcher,commandRegistryAccess,registrationEnvironment)
                        ).then(
                                CommandManager.literal("claim").executes(Commands::claim)
                                        .then(CommandManager.argument("from", ColumnPosArgumentType.columnPos()).requires(serverCommandSource -> {
                                            return serverCommandSource.hasPermissionLevel(2);
                                        })
                                                .then(CommandManager.argument("to",ColumnPosArgumentType.columnPos()).requires(serverCommandSource -> {
                                                            return serverCommandSource.hasPermissionLevel(2);
                                                        }).executes(Commands::operatorClaim)
                                                        )
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
                ).then(CommandManager.literal("reload").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4)).executes(commandContext -> {
                    Config.staticLoad();
                    return 0;
                }))

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

    private static int operatorClaim(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        ColumnPos from = ColumnPosArgumentType.getColumnPos(serverCommandSourceCommandContext,"from");
        ColumnPos to = ColumnPosArgumentType.getColumnPos(serverCommandSourceCommandContext,"to");
        int distX = Math.abs(from.x() - to.x());
        int distZ = Math.abs(from.z() - to.z());
        for (int i = 0; i < distX; i++) {
            int X = from.x() + i;
            for (int z = 0; z < distZ; z++) {
                int Z = from.z() + z;
                System.out.println("Claiming at Position: [" + X +", " + Z + "]");
                ChunkManager.claim(new Vector2i(X,Z),serverCommandSourceCommandContext.getSource().getPlayer(),true);
            }
        }
        serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Du hast nun alle Chunks von " + from + " bis " + to + " geclaimed"));
        return 0;
    }

    public static int leave(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {

        ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();
        if (player instanceof PlayerAccessor accessor){
            accessor.setTeam("");
            player.sendMessage(Text.literal("§2Du hast dein Team jetzt verlassen"));
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
        try{
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
                            Text join = Text.literal("§3[Beitreten]").fillStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/neocraft team accept " + sender.getName().getString())));
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
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }
    public static int getRemainingClaims(PlayerEntity player){
        try{
            if (player instanceof PlayerAccessor accessor){
                int alreadyClaimedChunks = ChunkManager.getAmountOfClaimedChunks(player);
                int maxClaims = accessor.getTeamManager().maxClaims;
                int mul = Config.CLAIMS_PER_PERSON.get();
                return (maxClaims * mul) - alreadyClaimedChunks;
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    public static int claim(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        try {
            ChunkPos pos = serverCommandSourceCommandContext.getSource().getPlayer().getChunkPos();
            Vector2i chunk = new Vector2i(pos.x,pos.z);
            ServerPlayerEntity player = serverCommandSourceCommandContext.getSource().getPlayer();
            sendClaimMessage(serverCommandSourceCommandContext,ChunkManager.claim(chunk,player,false));
            return 0;
        }catch (Throwable e){
            e.printStackTrace();
        }
        return 0;
    }

    public static int changeDisplayName(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        try {
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
                serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Der Displayname wurde zu §r").append(text).append(Text.literal("§2 geändert")));
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    public static void sendClaimMessage(CommandContext<ServerCommandSource> serverCommandSourceCommandContext, ClaimResult result) throws Exception{
        try {
            switch (result){
                case NOT_ENOUGHT_CLAIMS:
                    serverCommandSourceCommandContext.getSource().sendError(Text.literal("Dein Team hat keine Claims mehr übrig"));
                    break;
                case SUCCESS:
                    serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§2Dieser Chunk gehört nun dir, " + getRemainingClaims(serverCommandSourceCommandContext.getSource().getPlayer()) + " verbleibend"));
                    break;
                case ALREADY_CLAIMED:
                    break;
                case ALREADY_YOURS:
                    break;
                case ERROR_OCCURED:
                    serverCommandSourceCommandContext.getSource().sendMessage(Text.literal("§cEin Fehler ist aufgetreten beim Versuch einen diesen Chunk zu claimen, wenn dies wiederholt auftritt, bitte erstelle ein Ticket!"));
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static int createTeam(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        try {
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
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    public static int friendlyFire(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        try{
            serverCommandSource.sendMessage(Text.literal("Dies ist eine Funktion welche zum testen ist, und hat keinen Speziellen Einfluss"));
            if (serverCommandSource.getPlayer() instanceof PlayerAccessor accessor){
                if (accessor.isInATeam()){
                    TeamManager manager = accessor.getTeamManager();
                    manager.getSettings().friendlyFire = aBoolean;
                }
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    public static int attack_entities(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        try{
            if (serverCommandSource.getPlayer() instanceof  PlayerAccessor accessor){
                accessor.getTeamManager().getSettings().attack_entities = aBoolean;
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }

    public static int interact_entities(ServerCommandSource serverCommandSource, Boolean aBoolean) {
        try{
            if (serverCommandSource.getPlayer() instanceof  PlayerAccessor accessor){
                accessor.getTeamManager().getSettings().interact_entities = aBoolean;
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        return 0;
    }
}
