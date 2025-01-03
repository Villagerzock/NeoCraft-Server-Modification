package net.villagerzock.neocraft.Teams.Configurations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.villagerzock.neocraft.Commands;
import net.villagerzock.neocraft.Teams.Configuration;
import net.villagerzock.neocraft.Teams.TeamManager;

public class DisplayNameConfiguration extends Configuration<RequiredArgumentBuilder<ServerCommandSource, Text>> {

    @Override
    public ArgumentBuilder<ServerCommandSource, RequiredArgumentBuilder<ServerCommandSource, Text>> build(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        return CommandManager.argument("name", TextArgumentType.text(commandRegistryAccess)).executes(Commands::changeDisplayName);
    }
}
