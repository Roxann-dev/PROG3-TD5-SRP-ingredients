package prog3_td5.gestion_ingredients.entity;

import org.springframework.format.annotation.DurationFormat;

public class StockMovementCreation {
    private UnitType unit;
    private double quantity;
    private MovementType type;

    public StockMovementCreation(UnitType unit, double quantity, MovementType type) {
        this.unit = unit;
        this.quantity = quantity;
        this.type = type;
    }

    public UnitType getUnit() {
        return unit;
    }

    public void setUnit(UnitType unit) {
        this.unit = unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "StockMovementCreation{" +
                "unit=" + unit +
                ", quantity=" + quantity +
                ", type=" + type +
                '}';
    }
}
