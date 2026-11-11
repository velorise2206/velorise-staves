package com.velorise.staves.compat.jei;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.SwordImbueHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class VeloriseJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(VeloriseStavesMod.MODID, "jei_plugin");

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
                new ItemStack(VeloriseStavesMod.PYRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.HYDRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.ANEMO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.ELECTRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.DENDRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.CRYO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.GEO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.OMNI_ELEMENT.get())
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
            anvilRecipes.add(factory.createAnvilRecipe(left, allElements, outputs));
        }

        // 2. Anvil Element Repair examples
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.INFERNO_ROD.get(), VeloriseStavesMod.PYRO_ELEMENT.get()));
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.ARC_CASTER.get(), VeloriseStavesMod.OMNI_ELEMENT.get()));
        registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);

        // 3. Crafting examples for Omni Staff Upgrade CustomRecipe
        Ingredient basicStaves = Ingredient.of(
                VeloriseStavesMod.DRAGONFIRE_SCEPTER.get(), VeloriseStavesMod.RESONANT_ECHO.get(),
                VeloriseStavesMod.INFERNO_ROD.get(), VeloriseStavesMod.GHASTFIRE_STAFF.get(),
                VeloriseStavesMod.WITHER_SCEPTER.get(), VeloriseStavesMod.SHULKER_SEEKER.get(),
                VeloriseStavesMod.GUARDIAN_LENS.get(), VeloriseStavesMod.EVOKER_FANG_STAFF.get(),
                VeloriseStavesMod.ARC_CASTER.get(), VeloriseStavesMod.FROSTBOLT_WAND.get(),
                VeloriseStavesMod.WEBCASTER.get(), VeloriseStavesMod.WIND_STAFF.get()
        );
        Ingredient netheriteStaves = Ingredient.of(
                VeloriseStavesMod.NETHERITE_DRAGONFIRE_SCEPTER.get(), VeloriseStavesMod.NETHERITE_RESONANT_ECHO.get(),
                VeloriseStavesMod.NETHERITE_INFERNO_ROD.get(), VeloriseStavesMod.NETHERITE_GHASTFIRE_STAFF.get(),
                VeloriseStavesMod.NETHERITE_WITHER_SCEPTER.get(), VeloriseStavesMod.NETHERITE_SHULKER_SEEKER.get(),
                VeloriseStavesMod.NETHERITE_GUARDIAN_LENS.get(), VeloriseStavesMod.NETHERITE_EVOKER_FANG_STAFF.get(),
                VeloriseStavesMod.NETHERITE_ARC_CASTER.get(), VeloriseStavesMod.NETHERITE_FROSTBOLT_WAND.get(),
                VeloriseStavesMod.NETHERITE_WEBCASTER.get(), VeloriseStavesMod.NETHERITE_WIND_STAFF.get()
        );
        Ingredient omniIngredient = Ingredient.of(VeloriseStavesMod.OMNI_ELEMENT.get());

        List<CraftingRecipe> upgradeRecipes = List.of(
                new ShapelessRecipe(
                        new ResourceLocation(VeloriseStavesMod.MODID, "jei_omni_upgrade_basic"),
                        "omni_staff_upgrade",
                        CraftingBookCategory.EQUIPMENT,
                        new ItemStack(VeloriseStavesMod.OMNI_STAFF.get()),
                        NonNullList.of(Ingredient.EMPTY, basicStaves, omniIngredient)
                ),
                new ShapelessRecipe(
                        new ResourceLocation(VeloriseStavesMod.MODID, "jei_omni_upgrade_netherite"),
                        "omni_staff_upgrade",
                        CraftingBookCategory.EQUIPMENT,
                        new ItemStack(VeloriseStavesMod.NETHERITE_OMNI_STAFF.get()),
                        NonNullList.of(Ingredient.EMPTY, netheriteStaves, omniIngredient)
                )
        );
        registration.addRecipes(RecipeTypes.CRAFTING, upgradeRecipes);

        // 4. Elemental Essence Info tabs in JEI
        for (Element e : Element.values()) {
            registration.addIngredientInfo(
                    new ItemStack(VeloriseStavesMod.getElementItem(e)),
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.velorise_staves.element_info.base")
            );
        }
        registration.addIngredientInfo(
                new ItemStack(VeloriseStavesMod.OMNI_ELEMENT.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.velorise_staves.element_info.omni")
        );
    }

    private static IJeiAnvilRecipe createRepairExample(IVanillaRecipeFactory factory, Item staff, Item repairItem) {
        ItemStack damaged = new ItemStack(staff);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
        return factory.createAnvilRecipe(
                damaged,
                List.of(new ItemStack(repairItem)),
                List.of(new ItemStack(staff))
        );
    }
}
