package prog3_td5.gestion_ingredients.repository;

import org.springframework.stereotype.Repository;
import prog3_td5.gestion_ingredients.datasource.DataSource;
import prog3_td5.gestion_ingredients.entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {

    private final DataSource dataSource;

    public DishRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Dish findDishById(Integer id) {
        Connection connection = dataSource.getConnection();
        try {
            String sql = "SELECT id, name, dish_type, price FROM dish WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                dish.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
                dish.setIngredients(findDishIngredientByDishId(id));

                return dish;
            }
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            dataSource.closeConnection(connection);
        }
    }

    public Dish saveDish(Dish dish) {
        String upsertDishSql = """
        INSERT INTO dish (id, name, dish_type, price)
        VALUES (?, ?, ?::dish_type, ?)
        ON CONFLICT (id) DO UPDATE
        SET name = EXCLUDED.name,
            dish_type = EXCLUDED.dish_type,
            price = EXCLUDED.price
        RETURNING id
    """;

        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                ps.setObject(1, dish.getId(), Types.INTEGER);
                ps.setString(2, dish.getName());
                ps.setString(3, dish.getDishType().name());
                ps.setObject(4, dish.getPrice(), Types.DOUBLE);

                ResultSet rs = ps.executeQuery();
                rs.next();
                dishId = rs.getInt(1);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }

            if (dish.getIngredients() != null) {
                for (DishIngredient di : dish.getIngredients()) {
                    saveDishIngredient(
                            conn,
                            dishId,
                            di.getIngredient().getId(),
                            di.getQuantity(),
                            di.getUnit().name()
                    );
                }
            }
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<Dish> findDishsByIngredientName(String search) {
        List<Dish> dishes = new ArrayList<>();
        String sql = """
            SELECT DISTINCT d.* FROM dish d 
            JOIN dish_ingredient di ON d.id = di.id_dish 
            JOIN ingredient i ON di.id_ingredient = i.id 
            WHERE i.name ILIKE ?""";
        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish d = new Dish();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                d.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
                dishes.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche par ingrédient : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
        return dishes;
    }

    private List<DishIngredient> findDishIngredientByDishId(Integer dishId) {
        Connection connection = dataSource.getConnection();
        List<DishIngredient> result = new ArrayList<>();

        String sql = """
        SELECT
            i.id AS ingredient_id,
            i.name,
            i.price,
            i.category,
            di.quantity,
            di.unit
        FROM dish_ingredient di
        JOIN ingredient i ON i.id = di.id_ingredient
        WHERE di.id_dish = ?
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("ingredient_id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));
                ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                DishIngredient di = new DishIngredient(
                        null,
                        null,
                        ing,
                        rs.getDouble("quantity"),
                        UnitType.valueOf(rs.getString("unit"))
                );

                result.add(di);
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSource.closeConnection(connection);
        }
    }

    public void saveDishIngredient(Connection conn, Integer dishId, Integer ingredientId, Double quantity, String unit) {
        String sql = """
        INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity, unit)
        VALUES (?, ?, ?, ?::unit_type)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.setInt(2, ingredientId);
            ps.setDouble(3, quantity);
            ps.setString(4, unit);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lien Plat-Ingrédient : " + e.getMessage());
        }
    }

    public Double getDishCost(Integer dishId) {
        String sql = """
        SELECT SUM(di.quantity * i.price) as total_cost
        FROM dish_ingredient di
        JOIN ingredient i ON di.id_ingredient = i.id
        WHERE di.id_dish = ?
    """;

        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total_cost");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du calcul du coût du plat : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Double getGrossMargin(Integer dishId) {
        String sql = """
        SELECT 
            d.price - COALESCE(
                (SELECT SUM(di.quantity * i.price) 
                 FROM dish_ingredient di 
                 JOIN ingredient i ON di.id_ingredient = i.id 
                 WHERE di.id_dish = d.id), 
            0) as gross_margin
        FROM dish d
        WHERE d.id = ?
    """;

        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("gross_margin");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du calcul de la marge brute : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<Dish> findAllDishes() {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT * FROM dish";
        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish d = new Dish();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                d.setPrice(rs.getObject("price") != null ? rs.getDouble("price") : null);
                dishes.add(d);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération plats : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
        return dishes;
    }
}
