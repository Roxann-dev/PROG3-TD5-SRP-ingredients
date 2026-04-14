package prog3_td5.gestion_ingredients.service;
import org.springframework.stereotype.Service;
import prog3_td5.gestion_ingredients.entity.Dish;
import prog3_td5.gestion_ingredients.entity.DishIngredient;
import prog3_td5.gestion_ingredients.repository.DishRepository;

import java.util.List;

@Service
public class DishService {

    private final DishRepository dishRepository;

    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    public List<Dish> findAllDishes() {
        return dishRepository.findAllDishes();
    }

    public Dish updateIngredients(int id, List<DishIngredient> ingredients) {
        Dish dish = dishRepository.findDishById(id);
        dish.setIngredients(ingredients);
        return dishRepository.saveDish(dish);
    }
}
