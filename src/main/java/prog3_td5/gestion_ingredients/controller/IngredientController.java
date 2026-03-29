package prog3_td5.gestion_ingredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prog3_td5.gestion_ingredients.entity.Ingredient;
import prog3_td5.gestion_ingredients.entity.StockValue;
import prog3_td5.gestion_ingredients.exception.BadRequestException;
import prog3_td5.gestion_ingredients.exception.NotFoundException;
import prog3_td5.gestion_ingredients.repository.IngredientRepository;
import prog3_td5.gestion_ingredients.validator.IngredientValidator;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;
    private final IngredientValidator ingredientValidator;

    public IngredientController(IngredientRepository ingredientRepository, IngredientValidator ingredientValidator) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientValidator = ingredientValidator;
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
            ingredientValidator.validateIngredientExists(ingredient, id);
            return ResponseEntity.status(HttpStatus.OK).body(ingredient);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStockValue(
            @PathVariable int id,
            @RequestParam(required = false) String at,
            @RequestParam(required = false) String unit) {
        try {
            ingredientValidator.validateQueryParams(at, unit);
            Ingredient ingredient = ingredientRepository.findIngredientById(id);
            ingredientValidator.validateIngredientExists(ingredient, id);
            Instant instant = Instant.parse(at);
            StockValue stockValue = ingredient.getStockValueAt(instant);
            return ResponseEntity.status(HttpStatus.OK).body(stockValue);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}
