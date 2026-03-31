package prog3_td5.gestion_ingredients.repository;

import org.springframework.stereotype.Repository;
import prog3_td5.gestion_ingredients.datasource.DataSource;
import prog3_td5.gestion_ingredients.entity.*;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredient ORDER BY name LIMIT ? OFFSET ?";
        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur pagination : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
        return ingredients;
    }

    public List<Ingredient> findIngredientsByCriteria(String name, CategoryEnum cat, String dishName, int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        // Remplace la ligne 126 par :
        StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT i.* FROM ingredient i 
        LEFT JOIN dish_ingredient di ON i.id = di.id_ingredient 
        LEFT JOIN dish d ON di.id_dish = d.id 
        WHERE 1=1""");

        if (name != null) sql.append(" AND i.name ILIKE ?");
        if (cat != null) sql.append(" AND i.category = ?::category");
        if (dishName != null) sql.append(" AND d.name ILIKE ?");
        sql.append(" LIMIT ? OFFSET ?");

        Connection conn = dataSource.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (name != null) ps.setString(idx++, "%" + name + "%");
            if (cat != null) ps.setString(idx++, cat.name());
            if (dishName != null) ps.setString(idx++, "%" + dishName + "%");
            ps.setInt(idx++, size);
            ps.setInt(idx, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche critères : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
        return ingredients;
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) return List.of();

        Connection conn = dataSource.getConnection();
        List<Ingredient> savedIngredients = new ArrayList<>();

        String checkSql = "SELECT 1 FROM ingredient WHERE name = ?";
        String insertSql = "INSERT INTO ingredient (id, name, category, price) VALUES (?, ?, ?::category, ?) RETURNING id";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement checkPs = conn.prepareStatement(checkSql);
                 PreparedStatement insertPs = conn.prepareStatement(insertSql)) {

                for (Ingredient ing : newIngredients) {
                    checkPs.setString(1, ing.getName());
                    try (ResultSet rsCheck = checkPs.executeQuery()) {
                        if (rsCheck.next()) {
                            conn.rollback();
                            throw new RuntimeException("L'ingrédient existe déjà : " + ing.getName());
                        }
                    }

                    insertPs.setInt(1, ing.getId() != null ? ing.getId() : getNextSerialValue(conn, "ingredient", "id"));
                    insertPs.setString(2, ing.getName());
                    insertPs.setString(3, ing.getCategory().name());
                    insertPs.setDouble(4, ing.getPrice());

                    try (ResultSet rs = insertPs.executeQuery()) {
                        if (rs.next()) {
                            ing.setId(rs.getInt(1));
                            savedIngredients.add(ing);
                        }
                    }
                }
                conn.commit();
                return savedIngredients;

            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw new RuntimeException("Erreur SQL lors de l'insertion groupée : " + e.getMessage());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Ingredient saveIngredient(Ingredient toSave) {
        if (toSave == null) return null;

        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            String sqlIngredient = """
            INSERT INTO ingredient (id, name, price, category)
            VALUES (?, ?, ?, ?::category)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                price = EXCLUDED.price,
                category = EXCLUDED.category
            RETURNING id
        """;
            try (PreparedStatement ps = conn.prepareStatement(sqlIngredient)) {
                ps.setObject(1, toSave.getId(), Types.INTEGER);
                ps.setString(2, toSave.getName());
                ps.setDouble(3, toSave.getPrice());
                ps.setString(4, toSave.getCategory().name());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    toSave.setId(rs.getInt(1));
                }
            }
            saveStockMovements(conn, toSave);

            conn.commit();
            return toSave;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {}
            throw new RuntimeException("Erreur lors de la sauvegarde de l'ingrédient : " + e.getMessage());
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Ingredient findIngredientById(int id) {
        Ingredient ingredient = null;
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id"));
                ingredient.setName(rs.getString("name"));
                ingredient.setPrice(rs.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                ingredient.setStockMovementList(findMovementsByIngredientId(id));
            }
        } catch (SQLException e) {
            throw new RuntimeException(("L'ingrédient avec l'id " + id + " n'existe pas"));
        }
        return ingredient;
    }

    private Ingredient mapResultSetToIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient();
        ing.setId(rs.getInt("id"));
        ing.setName(rs.getString("name"));
        ing.setPrice(rs.getDouble("price"));
        ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));
        return ing;
    }

    private List<StockMovement> findMovementsByIngredientId(int idIng) {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT quantity, unit, type, creation_datetime FROM stock_movement WHERE id_ingredient = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIng);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StockMovement sm = new StockMovement();
                sm.setType(MovementType.valueOf(rs.getString("type")));
                sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                sm.setValue(new StockValue(rs.getDouble("quantity"), UnitType.valueOf(rs.getString("unit"))));
                list.add(sm);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private void saveStockMovements(Connection conn, Ingredient ingredient) throws SQLException {
        List<StockMovement> movements = ingredient.getStockMovementList();
        if (movements == null || movements.isEmpty()) return;

        String sqlMovement = """
        INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
        VALUES (?, ?, ?, ?::movement_type, ?::unit_type, ?)
        ON CONFLICT (id) DO NOTHING
        """;

        try (PreparedStatement psM = conn.prepareStatement(sqlMovement)) {
            for (StockMovement sm : movements) {
                psM.setObject(1, sm.getId(), Types.INTEGER);
                psM.setInt(2, ingredient.getId());
                psM.setDouble(3, sm.getValue().getQuantity());
                psM.setString(4, sm.getType().name());
                psM.setString(5, sm.getValue().getUnit().name());
                psM.setTimestamp(6, Timestamp.from(sm.getCreationDatetime()));

                psM.executeUpdate();
            }
        }
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String sequenceName;
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_get_serial_sequence(?, ?)")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) sequenceName = rs.getString(1);
                else throw new RuntimeException("No sequence found");
            }
        }
        String syncSql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))", sequenceName, columnName, tableName);
        conn.createStatement().executeQuery(syncSql);
        try (ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT nextval('%s')", sequenceName))) {
            rs.next();
            return rs.getInt(1);
        }
    }
    
    public List<StockMovement> findStockMovementsByIngredientIdBetween(int id, Instant from, Instant to){
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT id, creation_datetime, unit, type, quantity FROM stock_movement WHERE id_ingredient = ? \n" +
                "AND creation_datetime >= ? \n" +
                "AND creation_datetime <= ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StockMovement sm = new StockMovement();
                sm.setId(rs.getInt("id"));
                sm.setType(MovementType.valueOf(rs.getString("type")));
                sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                sm.setValue(new StockValue(rs.getDouble("quantity"), UnitType.valueOf(rs.getString("unit"))));
                list.add(sm);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<StockMovement> saveStockMovementsForIngredient(int ingredientId, List<StockMovementCreation> movements){
        List<StockMovement> list = new ArrayList<>();
        String sql = "INSERT INTO stock_movement (id_ingredient, quantity, type, unit, creation_datetime)\n" +
                "VALUES (?, ?, ?::movement_type, ?::unit_type, ?)\n" +
                "RETURNING id, creation_datetime";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (StockMovementCreation smc : movements) {
                ps.setInt(1, ingredientId);
                ps.setDouble(2, smc.getQuantity());
                ps.setString(3, smc.getType().name());
                ps.setString(4, smc.getUnit().name());
                ps.setTimestamp(5, Timestamp.from(Instant.now()));

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    sm.setType(smc.getType());
                    sm.setValue(new StockValue(smc.getQuantity(), smc.getUnit()));
                    list.add(sm);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
