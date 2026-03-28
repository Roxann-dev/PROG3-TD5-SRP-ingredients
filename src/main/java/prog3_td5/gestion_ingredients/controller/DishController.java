package prog3_td5.gestion_ingredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import prog3_td5.gestion_ingredients.entity.Dish;
import prog3_td5.gestion_ingredients.entity.DishIngredient;
import prog3_td5.gestion_ingredients.repository.DishRepository;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(dishRepository.findAllDishes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getClass().getName() + " : " + e.getMessage());
        }
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateIngredients(
            @PathVariable int id,
            @RequestBody(required = false) List<DishIngredient> ingredients) {
        if (ingredients == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body is required");
        }
        try {
            Dish dish = dishRepository.findDishById(id);
            dish.setIngredients(ingredients);
            Dish updated = dishRepository.saveDish(dish);
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Dish.id=" + id + " is not found");
        }
    }
}
