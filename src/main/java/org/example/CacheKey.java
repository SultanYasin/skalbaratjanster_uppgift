package org.example;

import java.time.Instant;

class CacheKey{
    public String method;
    public String path;

    public Instant updatedAt;

    public CacheKey(String method, String path) {
        this.method = method;
        this.path = path;
        this.updatedAt = Instant.now();
    }

    public boolean equals(CacheKey cacheKey){
        return (this.method.equals(cacheKey.method) && this.path.equals(cacheKey.path));
    }

    public void updateInstant(){
        this.updatedAt = Instant.now();
    }
}
