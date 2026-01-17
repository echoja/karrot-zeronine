-- 테이블 생성 스크립트

-- 딜 테이블
CREATE TABLE IF NOT EXISTS deals (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    original_price DECIMAL(12, 2) NOT NULL,
    deal_price DECIMAL(12, 2) NOT NULL,
    total_stock INT NOT NULL,
    remaining_stock INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    region_code VARCHAR(10) NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deal_status ON deals(status);
CREATE INDEX IF NOT EXISTS idx_deal_start_time ON deals(start_time);
CREATE INDEX IF NOT EXISTS idx_deal_region_code ON deals(region_code);
CREATE INDEX IF NOT EXISTS idx_deal_seller_id ON deals(seller_id);

-- 주문 테이블
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    order_number VARCHAR(20) NOT NULL UNIQUE,
    deal_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_buyer_id ON orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_order_deal_id ON orders(deal_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_number ON orders(order_number);

-- Foreign Key (실제 운영에서는 사용자 테이블 필요)
-- ALTER TABLE deals ADD CONSTRAINT fk_deal_seller FOREIGN KEY (seller_id) REFERENCES users(id);
-- ALTER TABLE orders ADD CONSTRAINT fk_order_deal FOREIGN KEY (deal_id) REFERENCES deals(id);
-- ALTER TABLE orders ADD CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES users(id);

-- 샘플 데이터 (개발용)
INSERT INTO deals (uuid, title, description, original_price, deal_price, total_stock, remaining_stock, start_time, end_time, region_code, seller_id, status)
VALUES
    ('sample-deal-001', '[테스트] 당근 마을 특산품 세트', '신선한 당근마을 특산품을 특가에 만나보세요!', 50000, 35000, 100, 100, NOW(), NOW() + INTERVAL '7 days', 'SEOUL-01', 1, 'PENDING'),
    ('sample-deal-002', '[테스트] 유기농 채소 박스', '직접 재배한 유기농 채소 모음', 30000, 22000, 50, 50, NOW(), NOW() + INTERVAL '3 days', 'SEOUL-01', 1, 'PENDING');
