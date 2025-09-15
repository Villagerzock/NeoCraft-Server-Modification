package net.villagerzock.neocraft.Teams;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class SinglePlayerTeamManager extends TeamManager{
    public SinglePlayerTeamManager(PlayerEntity player) {
        super(player.getDisplayName(), player.getNameForScoreboard(), player.getUuid());
    }
}
