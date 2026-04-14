package prog3_td5.gestion_ingredients.service;
import org.springframework.stereotype.Service;
import prog3_td5.gestion_ingredients.entity.*;
import prog3_td5.gestion_ingredients.repository.IngredientRepository;

import java.time.Instant;
import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public List<Ingredient> findIngredients(int page, int size) {
        return ingredientRepository.findIngredients(page, size);
    }

    public List<Ingredient> findListIngredients() {
        return ingredientRepository.findListIngredients();
    }

    public Ingredient findIngredientById(int id) {
        return ingredientRepository.findIngredientById(id);
    }

    public StockValue getStockValue(int id, Instant instant) {
        Ingredient ingredient = ingredientRepository.findIngredientById(id);
        return ingredient.getStockValueAt(instant);
    }

    public List<StockMovement> getStockMovements(int id, Instant from, Instant to) {
        return ingredientRepository.findStockMovementsByIngredientIdBetween(id, from, to);
    }

    public List<StockMovement> addStockMovements(int id, List<StockMovementCreation> movements) {
        return ingredientRepository.saveStockMovementsForIngredient(id, movements);
    }
}
