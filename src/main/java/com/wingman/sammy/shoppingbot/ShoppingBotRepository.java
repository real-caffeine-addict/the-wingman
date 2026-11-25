package com.wingman.sammy.shoppingbot;

import com.wingman.sammy.dto.SavedMedia;
import com.wingman.sammy.dto.StoreDtoMedia;
import com.wingman.sammy.shoppingbot.domain.ItemStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Repository
public class ShoppingBotRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Value("classpath:db/sql/insert_downloaded_media.sql")
    private Resource insertMediaSql;

    private String insertMediaStr;

    public ShoppingBotRepository (JdbcTemplate jdbc,
                                  NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @PostConstruct
    private void verifySqlStrings () throws IOException {
        insertMediaStr = StreamUtils.copyToString(insertMediaSql.getInputStream(), StandardCharsets.UTF_8);
    }

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

    public void setStatus(ItemStatus itemStatus, String id) {
        jdbc.update("""
                UPDATE shopping_items
                SET status = ?
                WHERE message_id = ?
                """, itemStatus.name(), id);
    }

    public void attachMessageId(String id, String item) {
        jdbc.update("""
                UPDATE shopping_items
                SET status = 'IN_PROGRESS', message_id = ?
                WHERE description = ?
                AND status = 'PENDING'
                """, id, item);
    }

    public SavedMedia insertMediaData(StoreDtoMedia storeDtoMedia) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(storeDtoMedia);
        namedJdbc.update(insertMediaStr, params, key, new String[]{"id"});
        Number keyValue = key.getKey();
        if (keyValue == null) {
            throw new IllegalStateException("Failed to retrieve generated media id");
        }
        return new SavedMedia(
                keyValue.longValue(),
                storeDtoMedia.path(),
                storeDtoMedia.fileName()

        );
    }
}
