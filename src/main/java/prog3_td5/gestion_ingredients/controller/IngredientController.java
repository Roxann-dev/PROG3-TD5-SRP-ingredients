package prog3_td5.gestion_ingredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prog3_td5.gestion_ingredients.entity.Ingredient;
import prog3_td5.gestion_ingredients.entity.StockMovementCreation;
import prog3_td5.gestion_ingredients.entity.StockValue;
import prog3_td5.gestion_ingredients.exception.BadRequestException;
import prog3_td5.gestion_ingredients.exception.NotFoundException;
import prog3_td5.gestion_ingredients.repository.IngredientRepository;
import prog3_td5.gestion_ingredients.service.IngredientService;
import prog3_td5.gestion_ingredients.validator.IngredientValidator;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientValidator ingredientValidator;

    public IngredientController(IngredientService ingredientService, IngredientValidator ingredientValidator) {
        this.ingredientService = ingredientService;
        this.ingredientValidator = ingredientValidator;
    }

    @GetMapping
    public ResponseEntity<?> findAllIngredients() {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ingredientService.findListIngredients());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findIngredientById(@PathVariable int id) {
        try {
            Ingredient ingredient = ingredientService.findIngredientById(id);
            ingredientValidator.validateIngredientExists(ingredient, id);
            return ResponseEntity.status(HttpStatus.OK).body(ingredient);
        } catch (NotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
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
            Ingredient ingredient = ingredientService.findIngredientById(id);
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

    @GetMapping("/{id}/stockMovements")
    public ResponseEntity<?> getStockMovements(
            @PathVariable int id,
            @RequestParam Instant from,
            @RequestParam Instant to){
        try {
            Ingredient ingredient = ingredientService.findIngredientById(id);
            ingredientValidator.validateIngredientExists(ingredient, id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ingredientService.getStockMovements(id, from, to));
        } catch (NotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/{id}/stockMovements")
    public ResponseEntity<?> addStockMovements(
            @PathVariable int id,
            @RequestBody List<StockMovementCreation> movements){
        try {
            Ingredient ingredient = ingredientService.findIngredientById(id);
            ingredientValidator.validateIngredientExists(ingredient, id);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ingredientService.addStockMovements(id, movements));
        } catch (NotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}
