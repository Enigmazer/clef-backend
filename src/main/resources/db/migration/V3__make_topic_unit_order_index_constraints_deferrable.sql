ALTER TABLE topics DROP CONSTRAINT uq_topics_unit_order;
ALTER TABLE topics ADD CONSTRAINT uq_topics_unit_order
    UNIQUE (unit_id,order_index) DEFERRABLE INITIALLY DEFERRED;