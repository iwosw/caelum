package org.iwoss.caelum.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    private static final List<ICommand> COMMANDS = new ArrayList<>();

    static {
        COMMANDS.add(new ReloadCommand());
        COMMANDS.add(new StatusCommand());
        COMMANDS.add(new CleanupCommand());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (ICommand command : COMMANDS) {
            command.register(dispatcher);
        }
    }
}