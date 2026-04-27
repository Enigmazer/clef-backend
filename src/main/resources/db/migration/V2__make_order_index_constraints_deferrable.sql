ALTER TABLE units DROP CONSTRAINT uq_units_subject_order;
ALTER TABLE units ADD CONSTRAINT uq_units_subject_order
    UNIQUE (subject_id, order_index) DEFERRABLE INITIALLY DEFERRED;