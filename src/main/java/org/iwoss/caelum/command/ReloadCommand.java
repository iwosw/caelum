package org.iwoss.caelum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.iwoss.caelum.CaelumConfig;

import java.util.function.Supplier;

public class ReloadCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("caelum")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(this::reloadConfig))
        );
    }

    private int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        CaelumConfig.SERVER_SPEC.afterReload();
        Supplier<Component> message = () -> Component.literal("§a[Caelum] Конфиг перезагружен. Новые значения применены.");
        ctx.getSource().sendSuccess(message, true);
        return 1;
    }
}