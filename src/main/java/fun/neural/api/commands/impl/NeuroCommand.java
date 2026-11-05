package fun.neural.api.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import fun.neural.api.commands.Command;
import fun.neural.api.utils.chat.ChatUtils;
import fun.neural.client.modules.impl.combat.components.rotations.NeuroRotation;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class NeuroCommand extends Command {

    public NeuroCommand() {
        super("neuro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .executes(context -> {
                    ChatUtils.sendMessage("Neuro: record — запись, train — обучение, status — статус, clear — сброс");
                    return SINGLE_SUCCESS;
                })
                .then(literal("record")
                        .executes(context -> {
                            if (NeuroRotation.INSTANCE.isRecording()) {
                                NeuroRotation.INSTANCE.stopRecording();
                                ChatUtils.sendMessage("Neuro: запись остановлена, датасет сохранён (" + NeuroRotation.INSTANCE.getSampleCount() + " сэмплов)");
                            } else {
                                NeuroRotation.INSTANCE.startRecording();
                                ChatUtils.sendMessage("Neuro: запись начата. Играй и бей цели как обычно, потом .neuro record для остановки");
                            }
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("train")
                        .executes(context -> {
                            NeuroRotation.INSTANCE.trainAsync();
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("status")
                        .executes(context -> {
                            ChatUtils.sendMessage("Neuro: " + NeuroRotation.INSTANCE.getStatus());
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("clear")
                        .executes(context -> {
                            NeuroRotation.INSTANCE.clearDataset();
                            ChatUtils.sendMessage("Neuro: датасет очищен");
                            return SINGLE_SUCCESS;
                        }));
    }
}
