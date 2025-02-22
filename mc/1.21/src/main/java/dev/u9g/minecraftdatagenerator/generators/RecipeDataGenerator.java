package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.item.Item;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeDataGenerator implements IDataGenerator {

    private static int getRawIdFor(Item item) {
        return DGU.getWorld().getRegistryManager().get(RegistryKeys.ITEM).getRawId(item);
    }

    @Override
    public String getDataName() {
        return "recipes";
    }

    @Override
    public JsonElement generateDataJson() {
        DynamicRegistryManager registryManager = DGU.getWorld().getRegistryManager();
        JsonObject finalObj = new JsonObject();
        Multimap<Integer, JsonObject> recipes = ArrayListMultimap.create();
        for (RecipeEntry<?> recipeE : Objects.requireNonNull(DGU.getWorld()).getRecipeManager().values()) {
            Recipe<?> recipe = recipeE.value();
            if (recipe instanceof ShapedRecipe sr) {
                generateShapedRecipe(registryManager, finalObj, sr, 0);
            } else if (recipe instanceof ShapelessRecipe sl) {
                var ingredients = new JsonArray();
                for (Ingredient ingredient : sl.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    ingredients.add(getRawIdFor(ingredient.getMatchingStacks()[0].getItem()));
                }
                var rootRecipeObject = new JsonObject();
                rootRecipeObject.add("ingredients", ingredients);
                var resultObject = new JsonObject();
                resultObject.addProperty("id", getRawIdFor(sl.getResult(registryManager).getItem()));
                resultObject.addProperty("count", sl.getResult(registryManager).getCount());
                rootRecipeObject.add("result", resultObject);
                recipes.put(getRawIdFor(sl.getResult(registryManager).getItem()), rootRecipeObject);
            }
        }
        recipes.forEach((a, b) -> {
            if (!finalObj.has(a.toString())) {
                finalObj.add(a.toString(), new JsonArray());
            }
            finalObj.get(a.toString()).getAsJsonArray().add(b);
        });
        return finalObj;
    }

    private void generateShapedRecipe(DynamicRegistryManager registryManager, JsonObject finalObj, ShapedRecipe sr, int n) {
        boolean hasIncremented = false;
        var ingredients = sr.getIngredients();
        List<Integer> ingr = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i >= ingredients.size()) {
                ingr.add(-1);
                continue;
            }
            var stacks = ingredients.get(i);
            var matching = stacks.getMatchingStacks(); // FIXME: fix when there are more than one matching stack
            if (matching.length == 0) {
                ingr.add(-1);
            } else if (matching.length > n){
                ingr.add(getRawIdFor(matching[n].getItem()));
            } else {
                ingr.add(getRawIdFor(matching[0].getItem()));
            }
            if (matching.length-1 > n && !hasIncremented) {
                generateShapedRecipe(registryManager, finalObj, sr, n+1);
                hasIncremented = true;
            }
        }

            JsonArray inShape = new JsonArray();

            var iter = ingr.iterator();
            for (int y = 0; y < sr.getHeight(); y++) {
                var jsonRow = new JsonArray();
                for (int z = 0; z < sr.getWidth(); z++) {
                    int value = iter.next();
                    jsonRow.add(value == -1 ? null : value);
                }
                inShape.add(jsonRow);
            }

        JsonObject finalRecipe = new JsonObject();
        finalRecipe.add("inShape", inShape);

        var resultObject = new JsonObject();
        resultObject.addProperty("id", getRawIdFor(sr.getResult(registryManager).getItem()));
        resultObject.addProperty("count", sr.getResult(registryManager).getCount());
        finalRecipe.add("result", resultObject);

        String id = ((Integer) getRawIdFor(sr.getResult(registryManager).getItem())).toString();

        if (!finalObj.has(id)) {
            finalObj.add(id, new JsonArray());
        }
        finalObj.get(id).getAsJsonArray().add(finalRecipe);
    }
}
