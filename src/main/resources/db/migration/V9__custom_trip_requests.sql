CREATE TABLE IF NOT EXISTS custom_trip_requests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(60),
    start_city VARCHAR(120) NOT NULL,
    destination_city VARCHAR(120) NOT NULL,
    preferred_date DATE,
    number_of_people INTEGER,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_custom_trip_requests_status ON custom_trip_requests(status);
CREATE INDEX IF NOT EXISTS idx_custom_trip_requests_created_at ON custom_trip_requests(created_at);

