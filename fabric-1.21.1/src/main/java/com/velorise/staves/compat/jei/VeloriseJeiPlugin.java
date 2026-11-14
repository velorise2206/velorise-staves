package com.velorise.staves.compat.jei;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.SwordImbueHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class VeloriseJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(EffectRenderingInventoryScreen.class, new IGuiContainerHandler<EffectRenderingInventoryScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EffectRenderingInventoryScreen<?> containerScreen) {
                return ClientAuraRenderer.getInventoryExtraAreas(containerScreen);
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        List<ItemStack> allElements = List.of(
                new ItemStack(VeloriseStavesMod.PYRO_ELEMENT),
                new ItemStack(VeloriseStavesMod.HYDRO_ELEMENT),
                new ItemStack(VeloriseStavesMod.ANEMO_ELEMENT),
                new ItemStack(VeloriseStavesMod.ELECTRO_ELEMENT),
                new ItemStack(VeloriseStavesMod.DENDRO_ELEMENT),
                new ItemStack(VeloriseStavesMod.CRYO_ELEMENT),
                new ItemStack(VeloriseStavesMod.GEO_ELEMENT),
                new ItemStack(VeloriseStavesMod.OMNI_ELEMENT)
        );
        List<String> keys = List.of("PYRO", "HYDRO", "ANEMO", "ELECTRO", "DENDRO", "CRYO", "GEO", SwordImbueHelper.OMNI_KEY);

        // 1. Anvil Sword Imbue previews (all Vanilla & Modded SwordItems)
        List<IJeiAnvilRecipe> anvilRecipes = new ArrayList<>();
        List<Item> allSwords = new ArrayList<>(List.of(
                Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD
        ));
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.SwordItem && !allSwords.contains(item)) {
                allSwords.add(item);
            }
        }
        for (Item sword : allSwords) {
            ItemStack left = new ItemStack(sword);
            List<ItemStack> outputs = new ArrayList<>(keys.size());
            for (String key : keys) {
                outputs.add(SwordImbueHelper.createImbuedCopy(left, key));
            }
            ResourceLocation itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword);
            String safeId = itemKey.getNamespace() + "_" + itemKey.getPath();
            anvilRecipes.add(factory.createAnvilRecipe(
                    left, allElements, outputs,
                    ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_imbue_" + safeId)
            ));
        }

        // 2. Anvil Element & Netherite Repair for all 15 Basic + 15 Netherite Staves
        Item[][] staffRepairPairs = new Item[][]{
                {VeloriseStavesMod.INFERNO_ROD, VeloriseStavesMod.NETHERITE_INFERNO_ROD, VeloriseStavesMod.PYRO_ELEMENT},
                {VeloriseStavesMod.GHASTFIRE_STAFF, VeloriseStavesMod.NETHERITE_GHASTFIRE_STAFF, VeloriseStavesMod.PYRO_ELEMENT},
                {VeloriseStavesMod.GUARDIAN_LENS, VeloriseStavesMod.NETHERITE_GUARDIAN_LENS, VeloriseStavesMod.HYDRO_ELEMENT},
                {VeloriseStavesMod.SPLENDOR_OF_TRANQUIL_WATERS, VeloriseStavesMod.NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS, VeloriseStavesMod.HYDRO_ELEMENT},
                {VeloriseStavesMod.FROSTBOLT_WAND, VeloriseStavesMod.NETHERITE_FROSTBOLT_WAND, VeloriseStavesMod.CRYO_ELEMENT},
                {VeloriseStavesMod.NIGHTS_FOCUS_STAFF, VeloriseStavesMod.NETHERITE_NIGHTS_FOCUS_STAFF, VeloriseStavesMod.CRYO_ELEMENT},
                {VeloriseStavesMod.ARC_CASTER, VeloriseStavesMod.NETHERITE_ARC_CASTER, VeloriseStavesMod.ELECTRO_ELEMENT},
                {VeloriseStavesMod.DRAGONFIRE_SCEPTER, VeloriseStavesMod.NETHERITE_DRAGONFIRE_SCEPTER, VeloriseStavesMod.ELECTRO_ELEMENT},
                {VeloriseStavesMod.SHULKER_SEEKER, VeloriseStavesMod.NETHERITE_SHULKER_SEEKER, VeloriseStavesMod.ELECTRO_ELEMENT},
                {VeloriseStavesMod.WITHER_SCEPTER, VeloriseStavesMod.NETHERITE_WITHER_SCEPTER, VeloriseStavesMod.DENDRO_ELEMENT},
                {VeloriseStavesMod.WEBCASTER, VeloriseStavesMod.NETHERITE_WEBCASTER, VeloriseStavesMod.DENDRO_ELEMENT},
                {VeloriseStavesMod.WIND_STAFF, VeloriseStavesMod.NETHERITE_WIND_STAFF, VeloriseStavesMod.ANEMO_ELEMENT},
                {VeloriseStavesMod.RESONANT_ECHO, VeloriseStavesMod.NETHERITE_RESONANT_ECHO, VeloriseStavesMod.ANEMO_ELEMENT},
                {VeloriseStavesMod.EVOKER_FANG_STAFF, VeloriseStavesMod.NETHERITE_EVOKER_FANG_STAFF, VeloriseStavesMod.GEO_ELEMENT},
                {VeloriseStavesMod.VORTEX_VANQUISHER, VeloriseStavesMod.NETHERITE_VORTEX_VANQUISHER, VeloriseStavesMod.GEO_ELEMENT},
                {VeloriseStavesMod.OMNI_STAFF, VeloriseStavesMod.NETHERITE_OMNI_STAFF, VeloriseStavesMod.OMNI_ELEMENT}
        };
        for (Item[] pair : staffRepairPairs) {
            Item basicStaff = pair[0];
            Item netheriteStaff = pair[1];
            Item elemEssence = pair[2];
            String basePath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(basicStaff).getPath();
            String nethPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(netheriteStaff).getPath();

            anvilRecipes.add(createRepairExample(factory, basicStaff, elemEssence, "jei_repair_elem_" + basePath));
            anvilRecipes.add(createRepairExample(factory, netheriteStaff, elemEssence, "jei_repair_elem_" + nethPath));
            if (elemEssence != VeloriseStavesMod.OMNI_ELEMENT) {
                anvilRecipes.add(createRepairExample(factory, basicStaff, VeloriseStavesMod.OMNI_ELEMENT, "jei_repair_omni_" + basePath));
                anvilRecipes.add(createRepairExample(factory, netheriteStaff, VeloriseStavesMod.OMNI_ELEMENT, "jei_repair_omni_" + nethPath));
            }
            anvilRecipes.add(createRepairExample(factory, netheriteStaff, Items.NETHERITE_INGOT, "jei_repair_netherite_" + nethPath));
        }
        registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);

        // 3. Omni Staff Smithing upgrades are native minecraft:smithing_transform recipes in datapack JSON.

        // 4. Elemental Essence Info tabs in JEI
        for (Element e : Element.values()) {
            registration.addIngredientInfo(
                    VeloriseStavesMod.getElementItem(e),
                    Component.translatable("jei.velorise_staves.element_info.base")
            );
        }
        registration.addIngredientInfo(
                VeloriseStavesMod.OMNI_ELEMENT,
                Component.translatable("jei.velorise_staves.element_info.omni")
        );
    }

    private static IJeiAnvilRecipe createRepairExample(IVanillaRecipeFactory factory, Item staff, Item repairItem, String id) {
        ItemStack damaged = new ItemStack(staff);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
        return factory.createAnvilRecipe(
                damaged,
                List.of(new ItemStack(repairItem)),
                List.of(new ItemStack(staff)),
                ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, id)
        );
    }
}
