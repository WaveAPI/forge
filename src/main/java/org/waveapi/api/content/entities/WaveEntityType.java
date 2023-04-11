package org.waveapi.api.content.entities;

import io.netty.util.DefaultAttributeMap;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.waveapi.Main;
import org.waveapi.api.WaveMod;
import org.waveapi.api.content.entities.renderer.WaveEntityRenderer;
import org.waveapi.api.misc.Side;
import org.waveapi.api.world.entity.EntityBase;
import org.waveapi.api.world.entity.EntityGroup;
import org.waveapi.api.world.entity.living.EntityLiving;
import org.waveapi.content.entity.EntityHelper;
import org.waveapi.utils.ClassHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "waveapi", bus = Mod.EventBusSubscriber.Bus.MOD)
public class WaveEntityType<T extends EntityBase> { // TODO: REWRITE THIS SHIT.

    private final WaveMod mod;
    public final Class<T> entity;
    public final Class<Entity> entityClass;

    private final String id;
    private final EntityType.Builder<Entity> preregister;
    public EntityType<? extends Entity> entityType;
    public EntityGroup type;
    public EntityBox box;

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        for (Map.Entry<EntityType<? extends LivingEntity>, WaveEntityType> s : attributeContainerMap.entrySet()) {
            event.put(s.getKey(), LivingEntity.createLivingAttributes().build());
        }
        attributeContainerMap = null;
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (Map.Entry<EntityType<? extends Entity>, EntityRendererFactory<Entity>> s : entityRenderers.entrySet()){
            event.registerEntityRenderer(s.getKey(), s.getValue());
        }
    }

    public static Map<net.minecraft.entity.EntityType<? extends Entity>,
    net.minecraft.client.render.entity.EntityRendererFactory<Entity>> entityRenderers = new HashMap<>();

    public static Map<EntityType<? extends LivingEntity>, WaveEntityType> attributeContainerMap = new HashMap<>();

    private static LinkedList<WaveEntityType<?>> toRegister = new LinkedList<>();

    public static IForgeRegistry<EntityType<?>> entityTypes = ForgeRegistries.ENTITY_TYPES;

    public static void register() {
        for (WaveEntityType<?> t : toRegister) {
            t.entityType = t.preregister.build(t.mod.name + ":" + t.id);

            if (EntityLiving.class.isAssignableFrom(t.entity)) {
                attributeContainerMap.put((EntityType<? extends LivingEntity>) t.entityType, t);
            }
            entityTypes.register(new Identifier(t.mod.name, t.id), t.entityType);


            if (Side.isClient()) {
                t.getEntityRenderer().register(t);
                entityRenderers.put(t.entityType, t.getEntityRenderer()::getRenderer);
            }

        }
        toRegister = null;
    }

    public WaveEntityRenderer getEntityRenderer() {
        return new WaveEntityRenderer();
    }


    @SuppressWarnings("unchecked")
    public WaveEntityType (String id, Class<T> entity, EntityGroup group, EntityBox box, WaveMod mod) {
        this.id = id;
        this.entity = entity;
        this.mod = mod;
        this.type = group;
        this.box = box;

        this.preregister = EntityType.Builder.create(group.to()).setDimensions(box.getDimensions().width, box.getDimensions().height);

        entityClass = (Class<Entity>) ClassHelper.LoadOrGenerateCompoundClass(entity.getTypeName() + "$mcEntity", new ClassHelper.Generator() {
            @Override
            public String[] getBaseMethods() {
                return EntityHelper.searchUpBase(entity);
            }

            @Override
            public List<String> getInterfaces() {
                return EntityHelper.searchUp(entity);
            }
        }, Main.bake);

        toRegister.add(this);
    }

    public void setMaxTrackingRange(int range) {
        this.preregister.setTrackingRange(range);
    }

    public WaveEntityType (String id, Class<T> entity, EntityBox box, WaveMod mod) {
        this(id, entity, EntityGroup.CREATURE, box, mod);
    }

    public WaveEntityType (String id, Class<T> entity,  WaveMod mod) {
        this(id, entity, new EntityBox.fixed(0.5f, 0.5f), mod);
    }

    public String getId() {
        return id;
    }

    public WaveMod getMod() {
        return mod;
    }


}
