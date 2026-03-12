ALTER TABLE trip ADD COLUMN deleted_at DATETIME NULL;
CREATE INDEX idx_trip_deleted_at ON trip (deleted_at);