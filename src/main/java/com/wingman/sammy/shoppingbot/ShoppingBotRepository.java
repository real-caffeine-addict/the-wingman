package com.wingman.sammy.shoppingbot;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public class ShoppingBotRepository {
    private final JdbcTemplate jdbc;

    public ShoppingBotRepository (JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void addItems (Set<String> input){
        for (String item : input){
            try {
                jdbc.update("INSERT INTO shopping_items (description) VALUES (?)", item);
            }catch (DuplicateKeyException e){
                System.out.println("הפריט" + item + " כבר רשום. אני מדלג עליו");
            }
        }
    }

    public List<String> readShoppingList () {
        String sql = "SELECT description from shopping_items WHERE status = 'PENDING'";
        return jdbc.queryForList(sql, String.class);
    }
}
