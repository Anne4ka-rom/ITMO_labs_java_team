-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users ( -- создаём таблицу users, если она ещё не существует
    login VARCHAR(50) PRIMARY KEY, -- логин пользователя, первичный ключ, максимум 50 символов
    password_hash VARCHAR(32) NOT NULL -- хэш пароля (md5 - 32 символа), обязательное поле
);

-- Создание таблицы транспортных средств
CREATE TABLE IF NOT EXISTS vehicles ( -- создаём таблицу vehicles, если она ещё не существует
    id SERIAL PRIMARY KEY, -- уникальный идентификатор, автоинкремент, первичный ключ
    name VARCHAR(255) NOT NULL, -- название транспортного средства, обязательное поле, максимум 255 символов
    coordinate_x DOUBLE PRECISION NOT NULL, -- координата x (дробное число двойной точности), обязательное поле
    coordinate_y INTEGER NOT NULL, -- координата y (целое число), обязательное поле
    creation_date DATE NOT NULL, -- дата создания записи, обязательное поле
    engine_power DOUBLE PRECISION NOT NULL, -- мощность двигателя (дробное число), обязательное поле
    capacity DOUBLE PRECISION NOT NULL, -- вместимость (дробное число), обязательное поле
    type VARCHAR(50) NOT NULL, -- тип транспортного средства (значение из перечисления VehicleType), обязательное поле
    fuel_type VARCHAR(50), -- тип топлива (значение из перечисления FuelType), может быть null
    owner_login VARCHAR(50) NOT NULL REFERENCES users(login) ON DELETE CASCADE -- логин владельца, внешний ключ на users, при удалении пользователя удаляются и его записи
);

-- Создание индекса для ускорения поиска по владельцу
CREATE INDEX IF NOT EXISTS idx_vehicles_owner ON vehicles(owner_login); -- индекс по полю owner_login для быстрых запросов типа WHERE owner_login = ?

-- Создание индекса для ускорения поиска по типу
CREATE INDEX IF NOT EXISTS idx_vehicles_type ON vehicles(type); -- индекс по полю type для быстрых запросов типа WHERE type = ? или фильтрации по типу