package org.waveapi.api.items.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.waveapi.Main;
import org.waveapi.api.WaveMod;
import org.waveapi.api.entities.entity.living.EntityPlayer;
import org.waveapi.api.items.ItemUseResult;
import org.waveapi.api.items.UseHand;
import org.waveapi.api.items.WaveItem;
import org.waveapi.api.items.WaveTab;
import org.waveapi.api.items.block.blockentities.TileEntityBlock;
import org.waveapi.api.items.block.blockentities.TileEntityCreation;
import org.waveapi.api.items.block.model.BlockModel;
import org.waveapi.api.items.drop.Drop;
import org.waveapi.api.items.drop.ItemDrop;
import org.waveapi.api.math.BlockPos;
import org.waveapi.api.misc.Side;
import org.waveapi.api.world.BlockState;
import org.waveapi.api.world.World;
import org.waveapi.content.items.CustomItemWrap;
import org.waveapi.content.items.blocks.BlockHelper;
import org.waveapi.content.items.blocks.BlockItemWrap;
import org.waveapi.content.items.blocks.CustomBlockWrap;
import org.waveapi.content.items.blocks.TileEntityWrapper;
import org.waveapi.content.resources.LangManager;
import org.waveapi.content.resources.ResourcePackManager;
import org.waveapi.content.resources.TagHelper;
import org.waveapi.utils.ClassHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.waveapi.Main.bake;

public class WaveBlock extends WaveItem {
    public Block block;
    private AbstractBlock.Settings blockSettings;

    public static Map<String,BlockEntityType<?>> blockEntities = new HashMap<>();


    public WaveBlock(String id, WaveMod mod, BlockMaterial material) {
        super(id, mod);
        this.blockSettings = AbstractBlock.Settings.of(material.mat);
    }

    public WaveBlock(String id, WaveMod mod) {
        this(id, mod, BlockMaterial.STONE);
    }

    public WaveBlock(Block block) {
        super(block.asItem());
        this.block = block;
    }

    public String[] _getBases() {
        return new String[] {CustomBlockWrap.class.getName()};
    }
    private static final IForgeRegistry<Block> blocks = ForgeRegistries.BLOCKS;


    @Override
    public void _registerLocal() {
        Block bl;
        try {
            bl = (Block) ClassHelper.LoadOrGenerateCompoundClass(
                    new ClassHelper.Generator() {
                        @Override
                        public String[] getBaseMethods() {
                            return _getBases();
                        }

                        @Override
                        public List<String> getInterfaces() {
                            return BlockHelper.searchUp(WaveBlock.this.getClass());
                        }
                    }).getConstructor(AbstractBlock.Settings.class, WaveBlock.class).newInstance(blockSettings, this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        block = bl;
        blocks.register(new Identifier(mod.name, id), bl);

        if (this instanceof TileEntityBlock) {
            try {
                Field type = block.getClass().getField("tileType");
                Field entityType = block.getClass().getField("entityType");
                final Class<? extends BlockEntity> tile = (Class<? extends BlockEntity>) ClassHelper.LoadOrGenerateCompoundClass(
                        new ClassHelper.Generator() {
                            @Override
                            public String[] getBaseMethods() {
                                return new String[] {TileEntityWrapper.class.getName()};
                            }

                            @Override
                            public List<String> getInterfaces() {
                                return BlockHelper.searchUpTile(((TileEntityBlock) WaveBlock.this).getTileEntity());
                            }
                        });
                entityType.set(block, tile);

                BlockEntityType<BlockEntity> entity = BlockEntityType.Builder.create((pos, state) -> {
                    try {
                        TileEntityCreation creation = new TileEntityCreation(tile, pos, state, (BlockEntityType) type.get(block));
                        return ((TileEntityBlock) this).getTileEntity().getConstructor(TileEntityCreation.class).newInstance (creation).blockEntity;
                    } catch (IllegalAccessException | InvocationTargetException |
                             InstantiationException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }, block).build(null);

                blockEntities.put(mod.name + ":" + id, entity);

                type.set(block, entity);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        this.base = new String[] {
                BlockItemWrap.class.getName(),
                CustomItemWrap.class.getName()
        };
        super.baseRegister();
    }

    public void enableRandomTick() {
        blockSettings.ticksRandomly();
    }
    public void onRandomTick(BlockState state, BlockPos pos, World world) {

    }

    public WaveBlock setTab(WaveTab tab) {
        this.tab = tab;
        return this;
    }

    public WaveBlock setHardness(float hardness) {
        this.blockSettings.hardness(hardness);
        return this;
    }

    public WaveBlock addTranslation(String language, String name) {
        if (Side.isClient() && bake) {
            LangManager.addTranslation(mod.name, language, "block." + mod.name + "." + id, name);
        }
        return this;
    }

    public WaveBlock setModels(BlockModel model) {
        if (Side.isClient() && bake) {
            model.buildBlock(ResourcePackManager.getInstance().getPackDir(), this, true, true, "");
        }
        return this;
    }

    public BlockState getDefaultState() {
        return new BlockState(block.getDefaultState());
    }

    public WaveBlock setDrop() {
        return setDrop(new Drop[] {new ItemDrop(this.mod.name + ":" + this.id)});
    }

    public ItemDrop getAsSimpleDrop() {
        return new ItemDrop(mod.name + ":" + id);
    }

    public WaveBlock setDrop(Drop[] drop) {
        if (!bake) {
            return this;
        }
        File file = new File(ResourcePackManager.getInstance().getPackDir(), "data/" + mod.name + "/loot_tables/blocks/" + this.id + ".json");
        file.getParentFile().mkdirs();
        StringBuilder builder = new StringBuilder("{\n" +
                "  \"type\": \"minecraft:block\",\n" +
                "  \"pools\": [\n" +
                "    {\n" +
                "      \"rolls\": 1.0,\n" +
                "      \"bonus_rolls\": 0.0,\n" +
                "      \"entries\": [\n");
        for (int i = 0 ; i < drop.length ; i++) {
            drop[i].write(builder);
            if (i < drop.length - 1) {
                builder.append(",");
            }
        }
        builder.append("""
                            ]
                        }
                    ]
                }""");


        try {
            Files.write(file.toPath(), builder.toString().getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public WaveBlock setDrop(Drop drop) {return this.setDrop(new Drop[]{drop}); }

    public WaveBlock setMiningLevelRequired(int level) {
        if (level > 0) {
            blockSettings.requiresTool();
        }
        if (!bake) return this;
        TagHelper.addTag("fabric", "blocks/needs_tool_level_" + level, this.mod.name + ":" + this.id);
        return this;
    }

    public WaveBlock makePickaxeEffective() {
        if (!bake) return this;
        TagHelper.addTag("minecraft", "blocks/mineable/pickaxe", this.mod.name + ":" + this.id);
        return this;
    }

    public WaveBlock makeAxeEffective() {
        if (!bake) return this;
        TagHelper.addTag("minecraft", "blocks/mineable/axe", this.mod.name + ":" + this.id);
        return this;
    }

    public WaveBlock makeShovelEffective() {
        if (!bake) return this;
        TagHelper.addTag("minecraft", "blocks/mineable/shovel", this.mod.name + ":" + this.id);
        return this;
    }

    public WaveBlock makeHoeEffective() {
        if (!bake) return this;
        TagHelper.addTag("minecraft", "blocks/mineable/hoe", this.mod.name + ":" + this.id);
        return this;
    }

    public ItemUseResult onUse(BlockState blockState, BlockPos pos, World world, EntityPlayer entityPlayer, UseHand useHand) {
        return null;
    }
}