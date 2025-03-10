package eu.pb4.placeholderstest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.LegacyFormattingParser;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;


public class TestMod implements ModInitializer {

    private static int perf(CommandContext<ServerCommandSource> context) {
        long placeholderTimeTotal = 0;
        long contextTimeTotal = 0;
        long tagTimeTotal = 0;
        long textTimeTotal = 0;
        Text output = null;
        var input = context.getArgument("text", String.class);
        ServerPlayerEntity player = context.getSource().getPlayer();

        int iter = 1024 * 20;

        try {
            for (int i = 0; i < iter; i++) {
                var time = System.nanoTime();
                var tags = TextNode.asSingle(
                        LegacyFormattingParser.ALL.parseNodes(
                                TextNode.asSingle(
                                        MarkdownLiteParserV1.ALL.parseNodes(
                                                TextNode.asSingle(
                                                        TextParserV1.DEFAULT.parseNodes(new LiteralNode(input))
                                                )
                                        )
                                )
                        )
                );
                tagTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                var placeholders = Placeholders.parseNodes(tags);
                placeholderTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                var ctx = ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player));
                contextTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                Text text = placeholders.toText(ctx, true);
                textTimeTotal +=  System.nanoTime() - time;
                output = text;
            }
            long total = tagTimeTotal + placeholderTimeTotal + textTimeTotal + contextTimeTotal;

            player.sendMessage(Text.literal(Text.Serializer.toJson(output)), false);
            player.sendMessage(Texts.parse(context.getSource(), output, context.getSource().getEntity(), 0), false);
            player.sendMessage(Text.literal(
                    "<FULL> Tag: " + ((tagTimeTotal / 1000) / 1000d) + " ms | " +
                            "Context: " + ((contextTimeTotal / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTimeTotal / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTimeTotal / 1000) / 1000d) + " ms | " +
                            "All: " + ((total / 1000) / 1000d) + " ms"
            ), false);

            player.sendMessage(Text.literal(
                    "<SINGLE> Tag: " + ((tagTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Context: " + ((contextTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "All: " + ((total / iter / 1000) / 1000d) + " ms"
            ), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            player.sendMessage(Placeholders.parseText(context.getArgument("text", Text.class), PlaceholderContext.of(player)), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test2(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Text text = TextParserUtils.formatText(context.getArgument("text", String.class));
            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);
            player.sendMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test3(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            var time = System.nanoTime();
            var tags = TextNode.asSingle(
                    LegacyFormattingParser.ALL.parseNodes(
                            TextNode.asSingle(
                                    MarkdownLiteParserV1.ALL.parseNodes(
                                            TextNode.asSingle(
                                                    TextParserV1.DEFAULT.parseNodes(new LiteralNode(context.getArgument("text", String.class)))
                                            )
                                    )
                            )
                    )
            );
            var tagTime = System.nanoTime() - time;
            time = System.nanoTime();

            var placeholders = Placeholders.parseNodes(tags);
            var placeholderTime = System.nanoTime() - time;
            time = System.nanoTime();

            Text text = placeholders.toText(ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player)), true);
            var textTime = System.nanoTime() - time;

            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);
            player.sendMessage(Texts.parse(context.getSource(), text, context.getSource().getEntity(), 0), false);
            player.sendMessage(Text.literal(
                      "Tag: " + ((tagTime / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTime / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTime / 1000) / 1000d) + " ms | " +
                            "All: " + (((tagTime + placeholderTime + textTime) / 1000) / 1000d) + " ms"
                    ), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test4Text(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Text text = Placeholders.parseText(
                    Placeholders.parseText(TextParserUtils.formatText(context.getArgument("text", String.class)), PlaceholderContext.of(player)),
                    Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", player.getName())
            );
            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);
            player.sendMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test4nodes(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Text text = Placeholders.parseNodes(
                    Placeholders.parseNodes(TextParserUtils.formatNodes(context.getArgument("text", String.class))),
                    Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", player.getName())
            ).toText(ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player)), true);
            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);
            player.sendMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test5(CommandContext<ServerCommandSource> context) {
        /*try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Text text = Placeholders.parseTextCustom(
                    TextParser.parse(context.getArgument("text", String.class)),
                    player,
                    Map.of(new Identifier("player"), (ctx) -> PlaceholderResult.value(Text.literal("").append(player.getName()).setStyle(Style.EMPTY.withColor(TextColor.parse(ctx.getArgument()))))), Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM);

            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);
            player.sendMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return 0;
    }

    private static int test6x(CommandContext<ServerCommandSource> context) {
        /*try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Text text = Placeholders.parseTextCustom(
                    TextParser.parse(context.getArgument("text", String.class)),
                    player,
                    Map.of(new Identifier("player"), (ctx) -> PlaceholderResult.value(Text.literal("").append(player.getName()).setStyle(Style.EMPTY.withColor(TextColor.parse(ctx.getArgument()))))), Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM);

            player.sendMessage(Text.literal(Text.Serializer.toJson(text)), false);

            // Never use it, pls
            player.sendMessage(Text.literal(eu.pb4.placeholders.old.util.TextParserUtils.convertToString(text)), false);

            player.sendMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return 0;
    }

    private static int test7(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();

            var text = Placeholders.parseText(Text.translatable("death.attack.outOfWorld", player.getDisplayName()), PlaceholderContext.of(player));
            player.sendMessage(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
            dispatcher.register(
                    literal("test").then(argument("text", TextArgumentType.text()).executes(TestMod::test))
            );

            dispatcher.register(
                    literal("test2").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test2))
            );

            dispatcher.register(
                    literal("test3").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test3))
            );

            dispatcher.register(
                    literal("perm").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::perf))
            );
            dispatcher.register(
                    literal("test4text").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test4Text))
            );
            dispatcher.register(
                    literal("test4nodes").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test4nodes))
            );
            dispatcher.register(
                    literal("test5").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test5))
            );

            dispatcher.register(
                    literal("test6ohno").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test6x))
            );

            dispatcher.register(
                    literal("test7").executes(TestMod::test7)
            );
        });
    }

}
