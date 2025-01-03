package net.villagerzock.neocraft.Teams;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.villagerzock.neocraft.Registries;

public class ConfigureHandler {
    public static LiteralArgumentBuilder<ServerCommandSource> build(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment){
        LiteralArgumentBuilder<ServerCommandSource> result = CommandManager.literal("configure");
        for (int i = 0; i < Registries.CONFIGURATIONS.size(); i++) {
            Configuration<?> configuration = Registries.CONFIGURATIONS.get(i);
            Identifier identifier = Registries.CONFIGURATIONS.getId(configuration);
            result.then(CommandManager.literal(identifier.getPath()).then(configuration.build(dispatcher,commandRegistryAccess,registrationEnvironment)));
        }
        return result;
    }
}
