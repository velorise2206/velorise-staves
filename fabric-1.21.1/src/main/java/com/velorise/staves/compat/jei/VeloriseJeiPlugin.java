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
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

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

        // 1. Anvil Sword Imbue previews
        List<IJeiAnvilRecipe> anvilRecipes = new ArrayList<>();
        Item[] sampleSwords = { Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD };
        for (Item sword : sampleSwords) {
            ItemStack left = new ItemStack(sword);
            List<ItemStack> outputs = new ArrayList<>(keys.size());
            for (String key : keys) {
                outputs.add(SwordImbueHelper.createImbuedCopy(left, key));
            }
            anvilRecipes.add(factory.createAnvilRecipe(
                    left, allElements, outputs,
                    ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_imbue_" + sword.builtInRegistryHolder().key().location().getPath())
            ));
        }

        // 2. Anvil Element Repair examples
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.INFERNO_ROD, VeloriseStavesMod.PYRO_ELEMENT, "jei_repair_pyro"));
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.ARC_CASTER, VeloriseStavesMod.OMNI_ELEMENT, "jei_repair_omni"));
        registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);

        // 3. Crafting examples for Omni Staff Upgrade CustomRecipe
        Ingredient basicStaves = Ingredient.of(
                VeloriseStavesMod.DRAGONFIRE_SCEPTER, VeloriseStavesMod.RESONANT_ECHO,
                VeloriseStavesMod.INFERNO_ROD, VeloriseStavesMod.GHASTFIRE_STAFF,
                VeloriseStavesMod.WITHER_SCEPTER, VeloriseStavesMod.SHULKER_SEEKER,
                VeloriseStavesMod.GUARDIAN_LENS, VeloriseStavesMod.EVOKER_FANG_STAFF,
                VeloriseStavesMod.ARC_CASTER, VeloriseStavesMod.FROSTBOLT_WAND,
                VeloriseStavesMod.WEBCASTER, VeloriseStavesMod.WIND_STAFF
        );
        Ingredient netheriteStaves = Ingredient.of(
                VeloriseStavesMod.NETHERITE_DRAGONFIRE_SCEPTER, VeloriseStavesMod.NETHERITE_RESONANT_ECHO,
                VeloriseStavesMod.NETHERITE_INFERNO_ROD, VeloriseStavesMod.NETHERITE_GHASTFIRE_STAFF,
                VeloriseStavesMod.NETHERITE_WITHER_SCEPTER, VeloriseStavesMod.NETHERITE_SHULKER_SEEKER,
                VeloriseStavesMod.NETHERITE_GUARDIAN_LENS, VeloriseStavesMod.NETHERITE_EVOKER_FANG_STAFF,
                VeloriseStavesMod.NETHERITE_ARC_CASTER, VeloriseStavesMod.NETHERITE_FROSTBOLT_WAND,
                VeloriseStavesMod.NETHERITE_WEBCASTER, VeloriseStavesMod.NETHERITE_WIND_STAFF
        );
        Ingredient omniIngredient = Ingredient.of(VeloriseStavesMod.OMNI_ELEMENT);

        List<RecipeHolder<CraftingRecipe>> upgradeHolders = List.of(
                new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_omni_upgrade_basic"),
                        new ShapelessRecipe("omni_staff_upgrade", CraftingBookCategory.EQUIPMENT,
                                new ItemStack(VeloriseStavesMod.OMNI_STAFF),
                                NonNullList.of(Ingredient.EMPTY, basicStaves, omniIngredient))
                ),
                new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_omni_upgrade_netherite"),
                        new ShapelessRecipe("omni_staff_upgrade", CraftingBookCategory.EQUIPMENT,
                                new ItemStack(VeloriseStavesMod.NETHERITE_OMNI_STAFF),
                                NonNullList.of(Ingredient.EMPTY, netheriteStaves, omniIngredient))
                )
        );
        registration.addRecipes(RecipeTypes.CRAFTING, upgradeHolders);

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
