package com.struggler.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.struggler.TheStruggler;
import com.struggler.world.entity.GuardAnimation;
import com.struggler.world.entity.MidlandGuard;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = TheStruggler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MidlandGuardCommands {
    private static final SimpleCommandExceptionType NO_GUARD = new SimpleCommandExceptionType(
            Component.literal("No living Midland Guard is within 32 blocks of the command source."));
    private static final SimpleCommandExceptionType NOT_A_GUARD = new SimpleCommandExceptionType(
            Component.literal("The selected entity is not a living Midland Guard."));

    private MidlandGuardCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("strugglerguard")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("action", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(GuardAnimation.Action.commandNames(), builder))
                                .executes(context -> execute(context.getSource(),
                                        StringArgumentType.getString(context, "action"), null))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> execute(context.getSource(),
                                                StringArgumentType.getString(context, "action"),
                                                EntityArgument.getEntity(context, "target"))))));
    }

    private static int execute(CommandSourceStack source, String rawAction, Entity selected)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        MidlandGuard guard = selected == null ? findNearest(source) : requireGuard(selected);
        String actionName = rawAction.toLowerCase(Locale.ROOT);
        if ("reset".equals(actionName)) {
            guard.resetPreviewAction();
            source.sendSuccess(() -> Component.literal("Midland Guard animation preview reset."), true);
            return 1;
        }

        GuardAnimation.Action action = GuardAnimation.Action.fromCommand(actionName);
        if (action == null) {
            throw new SimpleCommandExceptionType(Component.literal(
                    "Unknown preview action. Use idle, walk, run, attack, defend, or reset.")).create();
        }
        guard.setPreviewAction(action);
        source.sendSuccess(() -> Component.literal("Midland Guard animation preview: "
                + action.commandName() + " (" + action.durationTicks() + " ticks)."), true);
        return 1;
    }

    private static MidlandGuard requireGuard(Entity entity)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (entity instanceof MidlandGuard guard && guard.isAlive()) {
            return guard;
        }
        throw NOT_A_GUARD.create();
    }

    private static MidlandGuard findNearest(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Vec3 origin = source.getPosition();
        AABB search = new AABB(origin, origin).inflate(32.0D);
        return source.getLevel().getEntitiesOfClass(MidlandGuard.class, search, Entity::isAlive).stream()
                .min(Comparator.comparingDouble(guard -> guard.distanceToSqr(origin)))
                .orElseThrow(NO_GUARD::create);
    }
}
