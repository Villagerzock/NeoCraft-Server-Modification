package net.villagerzock.neocraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.villagerzock.neocraft.Teams.ChunkManager;
import net.villagerzock.neocraft.Teams.Configuration;
import net.villagerzock.neocraft.Teams.TeamManager;
import net.villagerzock.neocraft.config.Config;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Neocraft implements ModInitializer {
    public static final String MODID = "neocraft";
    private static final Map<ServerPlayerEntity, Vector2i> currentChunk = new HashMap<>();

    @Override
    public void onInitialize() {
        System.out.println("Main");
        //Registering Registries
        Configuration.staticLoad();
        //Registering Events
        CommandRegistrationCallback.EVENT.register(Commands::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(Neocraft::PlayerTick);
        AttackEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            return getResult(playerEntity,ChunkManager.getOwnedManager(playerEntity.getChunkPos()).getSettings().attack_entities);
        }));
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {

            return getResult(playerEntity,ChunkManager.getOwnedManager(playerEntity.getChunkPos()).getSettings().interact_entities);
        });
        //Static Loading Managers
        TeamManager.staticLoad();
        ChunkManager.staticLoad();
        Config.staticLoad();

    }
    private static ActionResult getResult(PlayerEntity playerEntity, boolean bool){
        if (ChunkManager.getOwnedManager(playerEntity.getChunkPos()) == null){
            System.out.println("Chunk Pos");
            return ActionResult.PASS;
        }
        if (bool){
            return ActionResult.PASS;
        }
        return isClaimed(playerEntity) ? ActionResult.FAIL : ActionResult.PASS;
    }
    private static boolean isClaimed(PlayerEntity player){
        if (player.hasPermissionLevel(4)){
            return false;
        }
        if (TeamManager.TEAMS.containsKey(ChunkManager.getOwner(ChunkManager.convert(player.getChunkPos())))){
            if (player instanceof PlayerAccessor accessor){
                return !accessor.getTeam().equals(ChunkManager.getOwner(ChunkManager.convert(player.getChunkPos())));
            }
        }
        return false;
    }
    private static void PlayerTick(MinecraftServer minecraftServer) {
        List<ServerPlayerEntity> players = minecraftServer.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity p : players){
            Vector2i vec = new Vector2i(p.getChunkPos().x,p.getChunkPos().z);
            if (currentChunk.containsKey(p)){
                Vector2i currentVec = currentChunk.get(p);
                if (!currentVec.equals(vec.x,vec.y)){
                    PlayerChangedChunk(p,minecraftServer,currentVec,vec);
                }
            }
            currentChunk.put(p,vec);

            if (p instanceof PlayerAccessor playerAccessor){
                String team = playerAccessor.getTeam();
                if (TeamManager.TEAMS.containsKey(team)){
                    TeamManager manager = TeamManager.TEAMS.get(team);
                    Text displayName = manager.getDisplayName();
                    Text playerName = p.getName();

                    Text customName = Text.empty().append(displayName).append(Text.literal(" §8|§r ")).append(playerName);

                    playerAccessor.setDisplayName(customName);
                }else {
                    playerAccessor.setDisplayName(p.getName());
                }
            }
        }
    }
    private static MutableText footer;
    private static MutableText header;

    public static void setFooter(MutableText footer) {
        Neocraft.footer = footer;
    }

    public static void setHeader(MutableText header) {
        Neocraft.header = header;
    }

    public static void updateTabList(MinecraftServer server){
        MutableText finalHeader = header.append(Text.literal("\n"));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
            if (player instanceof PlayerAccessor accessor){
                if (!accessor.getTeam().equals("")){
                    finalHeader.append(Text.literal("§3Du bist im Team: §r").append(TeamManager.TEAMS.get(accessor.getTeam()).getDisplayName()));
                }else {
                    finalHeader.append(Text.literal("§cDu bist in keinem Team"));
                }

            }
        }
    }
    private static void PlayerChangedChunk(ServerPlayerEntity player, MinecraftServer server, Vector2i oldChunk, Vector2i newChunk){
        if (!ChunkManager.isSame(oldChunk,newChunk)){
            String team = ChunkManager.getOwner(newChunk);
            Text text;
            if (team == ""){
                text = Text.literal("§2Du bist jetzt in der Wildnis");
            }else {
                text = Text.literal("§2Du bist jetzt im bereich von ").append(TeamManager.TEAMS.get(team).getDisplayName());
            }
            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(text));
        }
    }
}
