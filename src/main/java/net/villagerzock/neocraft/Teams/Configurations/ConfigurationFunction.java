package net.villagerzock.neocraft.Teams.Configurations;

import net.minecraft.server.command.ServerCommandSource;

public interface ConfigurationFunction<T> {
    int apply(ServerCommandSource source, T value);
}
