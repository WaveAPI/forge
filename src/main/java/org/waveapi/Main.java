package org.waveapi;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.waveapi.api.WaveLoader;
import org.waveapi.api.content.entities.WaveEntityType;
import org.waveapi.api.content.items.WaveItem;
import org.waveapi.api.content.items.block.WaveBlock;
import org.waveapi.api.content.items.recipes.WaveShapedRecipe;
import org.waveapi.api.misc.Side;
import org.waveapi.content.entity.EntityHelper;
import org.waveapi.content.resources.LangManager;
import org.waveapi.content.resources.ResourcePackManager;
import org.waveapi.content.resources.TagHelper;
import org.waveapi.utils.FileUtil;

import java.io.File;
import java.util.*;

@Mod("waveapi")

public class Main {
	public static final Logger LOGGER = LoggerFactory.getLogger("waveapi");

	public static final File mainFolder = new File("./waveAPI");

	public static boolean bake;


	public Main() {

		LOGGER.info("Initializing");
		long initialTime = System.currentTimeMillis();

		new ResourcePackManager();

		try {
			net.minecraft.client.MinecraftClient.getInstance();
			Side.isServer = false;
		} catch (Exception e) {
			Side.isServer = true;
		}

		Set<String> loaded = new HashSet<>();

		MixinConfigPlugin.allowedMixins = null;

		for (Map.Entry<String, WaveLoader.WrappedWaveMod> mod : WaveLoader.getMods().entrySet()) {
			bake = mod.getValue().changed;
			if (bake) {
				FileUtil.recursivelyDelete(new File(ResourcePackManager.getInstance().getPackDir(), "data/" + mod.getValue().mod.name));
				FileUtil.recursivelyDelete(new File(ResourcePackManager.getInstance().getPackDir(), "assets/" + mod.getValue().mod.name));
			}
			loaded.add(mod.getValue().mod.name);
			try {
				mod.getValue().mod.init();
			} catch (Exception e) {
				throw new RuntimeException("Failed because of waveAPI mod [" + mod.getValue().mod.name + "]", e);
			}
		}

		File[] files = new File(ResourcePackManager.getInstance().getPackDir(), "data").listFiles();
		if (files != null) {
			for (File f : files) {
				if (!loaded.contains(f.getName())) {
					FileUtil.recursivelyDelete(f);
				}
			}
		}

		files = new File(ResourcePackManager.getInstance().getPackDir(), "assets").listFiles();
		if (files != null) {
			for (File f : files) {
				if (!loaded.contains(f.getName())) {
					FileUtil.recursivelyDelete(f);
				}
			}
		}

		WaveShapedRecipe.build(new File(mainFolder, "resource_pack/data"));

		//WaveEntityType.register();

		TagHelper.write();

		if (Side.isClient()) {
			LangManager.write();
		}

		EntityHelper.entityPossibleInterfaces = null;

		LOGGER.info("Initializing took " + (System.currentTimeMillis() - initialTime) + "ms");
	}

	@SubscribeEvent
	public void register(RegisterEvent event) {
		event.register(ForgeRegistries.Keys.ITEMS, itemRegisterHelper -> {
			for (WaveItem item : WaveItem.toRegister) {
				itemRegisterHelper.register(new Identifier(item.getMod().name, item.getId()), item._getItem());
			}
			WaveItem.toRegister = null;
		});
		event.register(ForgeRegistries.Keys.BLOCKS, itemRegisterHelper -> {
			for (WaveBlock item : WaveBlock.blocks) {
				itemRegisterHelper.register(new Identifier(item.getMod().name, item.getId()), item.block);
			}
			WaveItem.toRegister = null;
		});
		event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, itemRegisterHelper -> {
			for (Map.Entry<String, BlockEntityType<?>> item : WaveBlock.blockEntities.entrySet()) {
				itemRegisterHelper.register(new Identifier(item.getKey()), item.getValue());
			}
			WaveBlock.blockEntities = null;
		});
	}

}