package prog3_td5.gestion_ingredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prog3_td5.gestion_ingredients.entity.Ingredient;
import prog3_td5.gestion_ingredients.entity.StockValue;
import prog3_td5.gestion_ingredients.repository.IngredientRepository;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    public IngredientController(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public ResponseEntity<List<Ingredient>> findIngredients(
            @RequestParam int page,
            @RequestParam int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ingredientRepository.findIngredients(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findIngredientById(@PathVariable int id) {
        try {
            Ingredient ingredient = ingredientRepository.findIngredientById(id);
            if (ingredient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ingredient.id=" + id + " is not found");
            }
            return ResponseEntity.status(HttpStatus.OK).body(ingredient);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStockValue(
            @PathVariable int id,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) String unit) {
        if (at == null || unit == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Either mandatory query parameter 'at' or 'unit' is not provided.");
        }
        try {
            Ingredient ingredient = ingredientRepository.findIngredientById(id);
            if (ingredient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Ingredient.id=" + id + " is not found");
            }
            Instant instant = Instant.parse(at);
            StockValue stockValue = ingredient.getStockValueAt(instant);
            return ResponseEntity.status(HttpStatus.OK).body(stockValue);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }
    }
}
