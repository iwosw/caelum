package org.iwoss.caelum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.iwoss.caelum.module.FluidValidator;
import org.iwoss.caelum.module.PlatformValidator;

import java.util.function.Supplier;

public class StatusCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("caelum")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(this::showStatus))
        );
    }

    private int showStatus(CommandContext<CommandSourceStack> ctx) {
        PlatformValidator pv = PlatformValidator.getInstance();
        int pending = pv != null ? pv.getPendingSize() : 0;
        int cache = pv != null ? pv.getCacheSize() : 0;
        int removedPlatforms = PlatformValidator.getRemovedPlatforms();
        int blockedWater = FluidValidator.getBlockedWater();

        Supplier<Component> message = () -> Component.literal(
                "§6[Caelum Status]\n" +
                        "§eОчередь платформ: §f" + pending + "\n" +
                        "§eКэш проверенных: §f" + cache + "\n" +
                        "§eУдалено платформ: §f" + removedPlatforms + "\n" +
                        "§eЗаблокировано воды: §f" + blockedWater
        );
        ctx.getSource().sendSuccess(message, false);
        return 1;
    }
}