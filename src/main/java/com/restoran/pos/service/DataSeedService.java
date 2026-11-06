package com.restoran.pos.service;

public interface DataSeedService {

    /** Yetkileri ve sistem rollerini eşitler; veritabanı boşsa örnek kullanıcı, masa, ürün ve belgeleri yükler. */
    void seed();
}
