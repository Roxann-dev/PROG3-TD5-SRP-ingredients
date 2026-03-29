package prog3_td5.gestion_ingredients.validator;

import org.springframework.stereotype.Component;
import prog3_td5.gestion_ingredients.entity.DishIngredient;
import prog3_td5.gestion_ingredients.entity.Ingredient;
import prog3_td5.gestion_ingredients.exception.BadRequestException;

import java.util.List;

@Component
public class DishValidator {
    public void validateIngredient(List<DishIngredient> ingredients) {
        if (ingredients == null){
            throw new BadRequestException("Ingredient list is null, request body is required");
        }
    }
}
