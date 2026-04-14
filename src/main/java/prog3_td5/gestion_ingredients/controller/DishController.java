package prog3_td5.gestion_ingredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prog3_td5.gestion_ingredients.entity.Dish;
import prog3_td5.gestion_ingredients.entity.DishIngredient;
import prog3_td5.gestion_ingredients.exception.BadRequestException;
import prog3_td5.gestion_ingredients.repository.DishRepository;
import prog3_td5.gestion_ingredients.service.DishService;
import prog3_td5.gestion_ingredients.validator.DishValidator;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService dishService;
    private final DishValidator dishValidator;

    public DishController(DishService dishService, DishValidator dishValidator) {
        this.dishService = dishService;
        this.dishValidator = dishValidator;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(dishService.findAllDishes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getClass().getName() + " : " + e.getMessage());
        }
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateIngredients(
            @PathVariable int id,
            @RequestBody(required = false) List<DishIngredient> ingredients) {
        try {
            dishValidator.validateIngredient(ingredients);
            Dish updated = dishService.updateIngredients(id, ingredients);
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Dish.id=" + id + " is not found");
        }
    }
}
