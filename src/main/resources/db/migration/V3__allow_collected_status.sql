ALTER TABLE shopping_items
    ADD CONSTRAINT chk_shopping_items_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COLLECTED', 'PURCHASED'));

EXECUTE IMMEDIATE 'ALTER TABLE shopping_items DROP CONSTRAINT ' ||
                  QUOTE_IDENT((
                      SELECT tc.constraint_name
                      FROM information_schema.table_constraints tc
                               JOIN information_schema.constraint_column_usage cu
                                    ON cu.constraint_schema = tc.constraint_schema
                                        AND cu.constraint_name = tc.constraint_name
                      WHERE tc.table_schema = CURRENT_SCHEMA
                        AND tc.table_name = 'SHOPPING_ITEMS'
                        AND tc.constraint_type = 'CHECK'
                        AND cu.column_name = 'STATUS'
                        AND tc.constraint_name <> 'CHK_SHOPPING_ITEMS_STATUS'
                  ));