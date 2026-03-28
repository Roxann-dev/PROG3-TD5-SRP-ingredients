package prog3_td5.gestion_ingredients.repository;

import org.springframework.stereotype.Repository;
import prog3_td5.gestion_ingredients.datasource.DataSource;
import prog3_td5.gestion_ingredients.entity.*;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private final DataSource dataSource;
    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    public OrderRepository(DataSource dataSource, DishRepository dishRepository, IngredientRepository ingredientRepository) {
        this.dataSource = dataSource;
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public Order saveOrder(Order orderToSave) {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            Instant now = Instant.now();

            checkStockAvailability(orderToSave, now);

            Integer orderId = insertOrderHeader(conn, orderToSave, now);

            insertOrderItems(conn, orderId, orderToSave.getDishOrders());

            conn.commit();
            orderToSave.setId(orderId);
            return orderToSave;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {}
            throw new RuntimeException("Échec de la commande : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Order findOrderByReference(String reference) {
        String sql = "SELECT id, reference, creation_datetime FROM \"Order\" WHERE reference = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt("id"));
                    order.setReference(rs.getString("reference"));
                    order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    order.setDishOrders(findDishOrdersByReference(reference));

                    return order;
                } else {
                    throw new RuntimeException("Commande introuvable pour la référence : " + reference);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération commande : " + e.getMessage());
        }
    }

    public List<DishOrder> findDishOrdersByReference(String reference) {
        List<DishOrder> list = new ArrayList<>();
        String sql = """
        SELECT dor.id, dor.quantity, d.id as dish_id, d.name, d.price, d.dish_type 
        FROM dish_order dor 
        JOIN "Order" o ON o.id = dor.id_order 
        JOIN dish d ON d.id = dor.id_dish 
        WHERE o.reference = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("dish_id"));
                    dish.setName(rs.getString("name"));
                    dish.setPrice(rs.getDouble("price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));

                    DishOrder dishOrder = new DishOrder();
                    dishOrder.setId(rs.getInt("id"));
                    dishOrder.setQuantity(rs.getInt("quantity"));
                    dishOrder.setDish(dish);

                    list.add(dishOrder);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des plats de la commande : " + e.getMessage());
        }
        return list;
    }

    private void checkStockAvailability(Order order, Instant t) {
        for (DishOrder dO : order.getDishOrders()) {
            Dish dish = dishRepository.findDishById(dO.getDish().getId());

            for (DishIngredient di : dish.getIngredients()) {
                Ingredient ing = ingredientRepository.findIngredientById(di.getIngredient().getId());
                double currentStockKg = ing.getStockValueAt(t).getQuantity();

                double requiredPerDishKg = UnitConverter.convertToKg(
                        ing.getName(),
                        di.getQuantity(),
                        di.getUnit()
                );

                double totalRequiredKg = requiredPerDishKg * dO.getQuantity();

                if (currentStockKg < totalRequiredKg) {
                    throw new RuntimeException("Stock insuffisant pour : " + ing.getName());
                }
            }
        }
    }

    private Integer insertOrderHeader(Connection conn, Order order, Instant now) throws SQLException {
        String sql = "INSERT INTO \"Order\" (reference, creation_datetime) VALUES (?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getReference());
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertOrderItems(Connection conn, Integer orderId, List<DishOrder> items) throws SQLException {
        String sql = "INSERT INTO dish_order (id_order, id_dish, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishOrder item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getDish().getId());
                ps.setInt(3, item.getQuantity());
                ps.executeUpdate();
            }
        }
    }
}
