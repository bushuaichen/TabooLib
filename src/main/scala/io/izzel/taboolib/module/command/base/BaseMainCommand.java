package io.izzel.taboolib.module.command.base;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.izzel.taboolib.TabooLib;
import io.izzel.taboolib.module.command.base.display.DisplayBase;
import io.izzel.taboolib.module.command.base.display.DisplayFlat;
import io.izzel.taboolib.module.locale.TLocale;
import io.izzel.taboolib.util.Ref;
import io.izzel.taboolib.util.Strings;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 主命令接口
 *
 * @author sky
 * @since 2018-05-07 21:38
 */
public abstract class BaseMainCommand implements CommandExecutor, TabExecutor {

    private DisplayBase display = new DisplayFlat();
    private PluginCommand registerCommand;
    private final List<Class<?>> linkClasses = new CopyOnWriteArrayList<>();
    private final List<BaseSubCommand> subCommands = new CopyOnWriteArrayList<>();

    public static void createCommandExecutor(PluginCommand command, BaseMainCommand baseMainCommand) {
        Preconditions.checkNotNull(command);
        baseMainCommand.setRegisterCommand(command);
        baseMainCommand.getRegisterCommand().setExecutor(baseMainCommand);
        baseMainCommand.getRegisterCommand().setTabCompleter(baseMainCommand);
        baseMainCommand.getLinkClasses().add(baseMainCommand.getClass());
        baseMainCommand.disguisedPlugin();
        loadCommandRegister(baseMainCommand);
    }

    public static void loadCommandRegister(BaseMainCommand baseMainCommand) {
        List<Method> methods = new ArrayList<>();
        List<CommandField> fields = new ArrayList<>();
        baseMainCommand.getLinkClasses()
                .forEach(clazz -> java.util.Arrays.stream(clazz.getDeclaredMethods())
                        .filter(method -> method.getAnnotation(SubCommand.class) != null).forEach(m -> {
                            m.setAccessible(true);
                            methods.add(m);
                        }));
        if (methods.size() > 0) {
            methods.sort(Comparator.comparingDouble(a -> a.getAnnotation(SubCommand.class).priority()));
            methods.forEach(method -> {
                try {
                    BaseSubCommand subCommand = null;
                    if (Arrays.equals(method.getParameterTypes(), new Class[]{CommandSender.class, String[].class})) {
                        subCommand = buildSubCommand(baseMainCommand, method)
                                .label(method.getName())
                                .annotation(method.getAnnotation(SubCommand.class));
                    } else if (Arrays.equals(method.getParameterTypes(), new Class[]{Player.class, String[].class})) {
                        subCommand = buildSubCommand(baseMainCommand, method)
                                .player()
                                .label(method.getName())
                                .annotation(method.getAnnotation(SubCommand.class));
                    }
                    if (subCommand != null) {
                        baseMainCommand.registerSubCommand(subCommand);
                    }
                } catch (Throwable ignored) {
                }
            });
        }
        baseMainCommand.getLinkClasses().forEach(clazz -> java.util.Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.getAnnotation(SubCommand.class) != null && field.getType().equals(BaseSubCommand.class)).forEach(field -> fields.add(new CommandField(field, clazz))));
        if (fields.size() > 0) {
            fields.sort(Comparator.comparingDouble(commandField -> commandField.getField().getAnnotation(SubCommand.class).priority()));
            fields.forEach(commandField -> {
                try {
                    BaseSubCommand subCommand = Ref.getField(commandField.getParent().newInstance(), commandField.getField(), BaseSubCommand.class);
                    if (subCommand != null) {
                        subCommand.label(commandField.getField().getName()).annotation(commandField.getField().getAnnotation(SubCommand.class));
                        baseMainCommand.registerSubCommand(subCommand);
                    }
                } catch (Throwable ignored) {
                }
            });
        }
    }

    public static BaseSubCommand buildSubCommand(BaseMainCommand baseMainCommand, Method method) {
        return new BaseSubCommand() {
            @Override
            public void onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
                try {
                    method.invoke(baseMainCommand, sender, args);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
    }

    public void registerSubCommand(BaseSubCommand subCommand) {
        Preconditions.checkNotNull(subCommand);
        Preconditions.checkArgument(subCommand.getLabel() != null, "Command label can not be null");
        Preconditions.checkArgument(subCommand.getArguments() != null, "Command arguments can not be null");
        subCommands.add(subCommand.mainCommand(this));
    }

    public void onCommandHelp(CommandSender sender, Command command, String label, String[] args) {
        display.displayHead(sender, this, label);
        for (BaseSubCommand subCommand : subCommands) {
            if (subCommand.getType().isType(sender) && sender.hasPermission(subCommand.getPermission()) && !subCommand.hideInHelp()) {
                display.displayParameters(sender, subCommand, label);
            }
        }
        display.displayBottom(sender, this, label);
    }

    /**
     * 5.38 update
     * 命令补全扩展方法
     *
     * @param sender   用户实例
     * @param command  指令
     * @param argument 参数
     * @return 补全结果
     */
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String command, @NotNull String argument) {
        return null;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 1) {
            List<String> label = Lists.newArrayList();
            subCommands.stream().filter(subCommand -> subCommand != null && !subCommand.hideInHelp() && subCommand.hasPermission(sender)).forEach(i -> {
                label.add(i.getLabel());
                label.addAll(Lists.newArrayList(i.getAliases()));
            });
            return label.stream().filter(i -> args[0].isEmpty() || i.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        for (BaseSubCommand subCommand : subCommands) {
            Argument[] arguments = subCommand.getArguments();
            if (subCommand.isCommand(args[0]) && args.length - 1 <= arguments.length) {
                CommandTab commandTab = arguments[args.length - 2].getTab();
                if (commandTab != null) {
                    List<String> run = commandTab.run();
                    if (run != null) {
                        return run.stream()
                                .filter(i -> args[args.length - 1].isEmpty() || i.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
                return onTabComplete(sender, subCommand.getLabel(), arguments[args.length - 2].getName());
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            onCommandHelp(sender, command, label, args);
        } else {
            for (BaseSubCommand subCommand : subCommands) {
                if (subCommand == null || !(args[0].equalsIgnoreCase(subCommand.getLabel()) || java.util.Arrays.stream(subCommand.getAliases()).anyMatch(args[0]::equalsIgnoreCase)) || !subCommand.hasPermission(sender)) {
                    continue;
                }
                if (!subCommand.getType().isType(sender)) {
                    TLocale.sendTo(sender, "COMMANDS.INTERNAL.TYPE-ERROR", args[0], TLocale.asString(sender, "COMMANDS.INTERNAL.TYPE-" + subCommand.getType()), registerCommand.getPlugin().getName());
                    return true;
                }
                String[] subCommandArgs = removeFirst(args);
                if (subCommand.isParameterConform(subCommandArgs)) {
                    subCommand.onCommand(sender, command, label, subCommandArgs);
                } else {
                    display.displayErrorUsage(sender, this, args[0], subCommand.getCommandString(sender, label));
                }
                return true;
            }
            new BukkitRunnable() {

                @Override
                public void run() {
                    try {
                        List<BaseSubCommand> commandCompute = subCommands.stream().filter(x -> x != null && x.hasPermission(sender)).sorted((b, a) -> Double.compare(Strings.similarDegree(args[0], a.getLabel()), Strings.similarDegree(args[0], b.getLabel()))).collect(Collectors.toList());
                        if (commandCompute.size() > 0) {
                            display.displayErrorCommand(sender, BaseMainCommand.this, args[0], commandCompute.get(0).getCommandString(sender, label));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(TabooLib.getPlugin());
        }
        return true;
    }

    @Override
    public String toString() {
        return "BaseMainCommand{" +
                "display=" + display +
                ", registerCommand=" + registerCommand +
                ", linkClasses=" + linkClasses +
                ", subCommands=" + subCommands +
                '}';
    }

    public String getCommandTitle() {
        return "§e§l----- §6§l" + registerCommand.getPlugin().getName() + " Commands §e§l-----";
    }

    public void setRegisterCommand(PluginCommand registerCommand) {
        Preconditions.checkNotNull(registerCommand);
        this.registerCommand = registerCommand;
    }

    public PluginCommand getRegisterCommand() {
        return registerCommand;
    }

    public List<Class<?>> getLinkClasses() {
        return linkClasses;
    }

    public List<BaseSubCommand> getSubCommands() {
        return subCommands;
    }

    public DisplayBase getDisplay() {
        return display;
    }

    public void setDisplay(DisplayBase display) {
        Preconditions.checkNotNull(display);
        this.display = display;
    }

    private void disguisedPlugin() {
        try {
            linkClasses.forEach(clazz -> disguisedPlugin(clazz, registerCommand.getPlugin()));
        } catch (Throwable ignored) {
        }
    }

    private void disguisedPlugin(Class<?> targetClass, Plugin plugin) {
        try {
            Field pluginField = targetClass.getClassLoader().getClass().getDeclaredField("plugin");
            Ref.putField(targetClass.newInstance(), pluginField, plugin);
        } catch (Exception ignored) {
        }
    }

    private String[] removeFirst(String[] args) {
        if (args.length < 2) {
            return new String[0];
        }
        String[] newArray = new String[args.length - 1];
        System.arraycopy(args, 1, newArray, 0, args.length - 1);
        return newArray;
    }
}
