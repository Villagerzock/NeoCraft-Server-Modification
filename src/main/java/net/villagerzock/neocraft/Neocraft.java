package net.villagerzock.neocraft;

import com.flowpowered.math.vector.Vector2d;
import com.technicjelle.BMUtils.Cheese;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.villagerzock.WebHook.Embed;
import net.villagerzock.WebHook.EmbedMessage;
import net.villagerzock.neocraft.Teams.ChunkManager;
import net.villagerzock.neocraft.Teams.Configuration;
import net.villagerzock.neocraft.Teams.TeamManager;
import net.villagerzock.neocraft.config.Config;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Neocraft implements ModInitializer {
    public static final String MODID = "neocraft";
    private static final Map<ServerPlayerEntity, Vector2i> currentChunk = new HashMap<>();
    public static BlueMapAPI blueMapAPI;
    public static MinecraftServer server;
    public static Map<String,MarkerSet> teamSets = new HashMap<>();
    private static Map<String,List<ChunkPos>> shapesChunks = new HashMap<>();
    public static final Logger logger = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        //Initialize BlueMap Plugin
        BlueMapAPI.onEnable(blueMapAPI -> {
            Neocraft.blueMapAPI = blueMapAPI;
            //Static Loading Managers
            TeamManager.staticLoad();
            ChunkManager.staticLoad(server);
            Config.staticLoad();
        });

        System.out.println("Main");
        //Registering Registries
        Configuration.staticLoad();
        //Registering Events
        ServerLifecycleEvents.SERVER_STARTED.register(minecraftServer -> {
            server = minecraftServer;
        });
        CommandRegistrationCallback.EVENT.register(Commands::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(Neocraft::PlayerTick);
        ServerMessageEvents.CHAT_MESSAGE.register(Neocraft::playerSentMessage);
        AttackEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (!(entity instanceof PlayerEntity)){
                return ActionResult.PASS;
            }
            if (ChunkManager.getOwnedManager(world,playerEntity.getChunkPos()) == null){
                System.out.println("Chunk Pos");
                return ActionResult.PASS;
            }
            if (!ChunkManager.getOwnedManager(world,playerEntity.getChunkPos()).settings.friendlyFire){
                return ActionResult.PASS;
            }
            return isClaimed(playerEntity) ? ActionResult.FAIL : ActionResult.PASS;
        }));
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {

            return getResult(world,playerEntity,ChunkManager.getOwnedManager(world,playerEntity.getChunkPos()).getSettings().interact_entities);
        });

    }

    private static void playerSentMessage(SignedMessage signedMessage, ServerPlayerEntity player, MessageType.Parameters parameters) {
        try {
            WebHook webHook = new WebHook(Config.CHAT_MESSAGE_WEBHOOK.get());
            webHook.sendMessage(new EmbedMessage.Builder().addEmbed(new Embed.Builder().setAuthor(player.getName().getString(),null,null).setBody("Sent A Message",signedMessage.getSignedContent(),null,Embed.YELLOW).build()).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addMarkerForChunk(ChunkPos pos, String team, ChunkManager.ChunkData data){
        if (!shapesChunks.containsKey(team)){
            shapesChunks.put(team,new ArrayList<>());
        }
        shapesChunks.get(team).add(pos);
        if (!teamSets.containsKey(team)){
            MarkerSet set = MarkerSet.builder().label(team).build();
            teamSets.put(team,set);
        }
        MarkerSet markerSet = teamSets.get(team);

        com.flowpowered.math.vector.Vector2i[] chunkCoordinates = new com.flowpowered.math.vector.Vector2i[shapesChunks.get(team).size()];
        for (int i = 0; i < chunkCoordinates.length; i++) {
            chunkCoordinates[i] = new com.flowpowered.math.vector.Vector2i(shapesChunks.get(team).get(i).x,shapesChunks.get(team).get(i).z);
        }
        Collection<Cheese> cheeses = Cheese.createPlatterFromChunks(chunkCoordinates);
        Shape.Builder shape;
        int i = 0;
        for (Cheese cheese : cheeses){
            ExtrudeMarker extrudeMarker = ExtrudeMarker.builder()
                    .label(TeamManager.get(team).getDisplayName().getString() + " " + i)
                    .fillColor(new Color(17,160,255,100f / 255f))
                    .lineColor(new Color(17,160,255))
                    .lineWidth(2)
                    .shape(cheese.getShape(),-64f,319f)
                    .holes(cheese.getHoles().toArray(Shape[]::new))
                    .depthTestEnabled(true)
                    .build();
            extrudeMarker.setDetail(getTextAsHTML(TeamManager.get(team).getDisplayName()));
            markerSet.getMarkers().put(team,extrudeMarker);
        }




        blueMapAPI.getWorld(data.world).ifPresent(blueMapWorld -> {
            for (BlueMapMap map : blueMapWorld.getMaps()){
                map.getMarkerSets().put("claimed-chunks",markerSet);
            }
        });
    }
    private static String getTextAsHTML(Text text){
        StringBuilder detail = new StringBuilder();
        List<Text> texts = new ArrayList<>();
        texts.add(text);
        texts.addAll(MutableText.of(text.getContent()).getSiblings());

        for (Text childText : texts){
            String Color = childText.getStyle().getColor() == null ? "#ffffff" : childText.getStyle().getColor().getHexCode();
            detail.append("<span style='color:").append(Color).append(";'>").append(childText.getString()).append("</span>");
        }
        return detail.toString();
    }
    private static ActionResult getResult(World world, PlayerEntity playerEntity, boolean bool){
        if (ChunkManager.getOwnedManager(world,playerEntity.getChunkPos()) == null){
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
        if (TeamManager.TEAMS.containsKey(ChunkManager.getOwner(player.getWorld(),ChunkManager.convert(player.getChunkPos())))){
            if (player instanceof PlayerAccessor accessor){
                return !accessor.getTeam().equals(ChunkManager.getOwner(player.getWorld(),ChunkManager.convert(player.getChunkPos())));
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
        if (!ChunkManager.isSame(player.getWorld(),oldChunk,newChunk)){
            TeamManager team = ChunkManager.getOwnedManager(player.getWorld(),ChunkManager.convert(newChunk));
            Text text;
            if (team == TeamManager.WILDERNESS){
                text = Text.literal("§2Du bist jetzt in der Wildnis");
            }else {
                text = Text.literal("§2Du bist jetzt im bereich von ").append(team.getDisplayName());
            }
            player.networkHandler.sendPacket(new OverlayMessageS2CPacket(text));
        }
    }
}
