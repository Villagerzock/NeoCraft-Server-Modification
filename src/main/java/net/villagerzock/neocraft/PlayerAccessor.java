package net.villagerzock.neocraft;

import net.minecraft.text.Text;
import net.villagerzock.neocraft.Teams.TeamManager;

public interface PlayerAccessor {
    String getTeam();
    TeamManager getTeamManager();
    void setTeam(String name);
    void setDisplayName(Text text);
    boolean isInATeam();
}
