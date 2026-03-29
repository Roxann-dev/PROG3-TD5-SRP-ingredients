package prog3_td5.gestion_ingredients.validator;

import org.springframework.stereotype.Component;
import prog3_td5.gestion_ingredients.exception.BadRequestException;
import prog3_td5.gestion_ingredients.exception.NotFoundException;

@Component
public class IngredientValidator {

    public void validateQueryParams(String at, String unit) {
        if (at == null || unit == null) {
            throw new BadRequestException("Either mandatory query parameter 'at' or 'unit' is not provided.");
        }
    }

    public void validateIngredientExists(Object ingredient, int id) {
        if (ingredient == null) {
            throw new NotFoundException("Ingredient.id=" + id + " is not found");
        }
    }
}