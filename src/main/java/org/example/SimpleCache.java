package org.example;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Väldigt enkel cache mest för att visa på principen.
//Detta är en cache som uppdateras vid varje request men den absolut första requesten efter uppdatering visar en gammal version. Max 24h. Alltså uppdaterar man cache åt varandra.
//Vi skulle kanske egentligen också köra denna på en egen tråd.
//Bäst vore att använda en FullHttpRequest och spara den.
//Det är också lite problematiskt när vi behöver vara inloggade på alla requests. För då borde vi kolla så att JWTn faktiskt är aktiv osv och inte leverera ett svar om den ej är det.
public class SimpleCache {

    //Själva lagringen för cachen.
    static HashMap<CacheKey, ByteBuf> inMemoryCache = new HashMap<>();

    //För att veta om vi ska spara i cache eller retunera sparar vi alla kanaler som redan fått ett cachat svar.
    static HashMap<String, CacheKey> cachedReturnedOnChannel = new HashMap<>();
    public static void storeChannel(String channelId, CacheKey cacheKey){
        cachedReturnedOnChannel.put(channelId, cacheKey);
    }

    //Detta är en lista av alla sparade paths. Jag har valt endast product/all pga ovanstående.
    static List<String> cachedPaths = List.of(
            "/product/all"
    );

    //Vi tar in ett channel och om det är en godkänd path och en godkänd metod sparar vi cachen.
    public static boolean storeCache(String channelId, ByteBuf buf){
        if(cachedReturnedOnChannel.containsKey(channelId)){
            CacheKey key = cachedReturnedOnChannel.get(channelId);
            cachedReturnedOnChannel.remove(channelId);
            if(key.method.equals("get") && cachedPaths.contains(key.path)){
                boolean containsKey = inMemoryCache.containsKey(key);
                key.updateInstant();
                inMemoryCache.put(key, buf);
                System.out.printf("Cache updated for path %s\n", key.path);
                return !containsKey;
            }
        }
        return true;
    }

    //Vi hämtar cache genom att skicka in en cachekey. Vi kollar av om metod/path finns i inMemoryCache. Vi kollar också så att det vi lagrat är nyare än 1 dygn.
    //Om båda kriterierna uppfylls levererar vi cachen. Annars null.
    public static ByteBuf getBuf(CacheKey key){
        for (CacheKey k : inMemoryCache.keySet()) {
            if(key.equals(k) && k.updatedAt.isAfter(Instant.now().minusSeconds(86400))){
                return inMemoryCache.get(k);
            }
        }
        return null;
    }

    //Metod för att berätta att vi väntar på svar från denna kanalen och denna metoden.

    //För att göra det enkelt att bygga cachekeyn kan man använda denna metoden.
    //Vi tar vår bytebuf och tar ut det vi behöver för att bygga en cachekey.
    //För användaren är det inte viktigt om vi får ut en korrekt cachekey eller inte så därför levererar vi alltid en.
    //Om den sedan kan användas/sparas testat när vi försöker hämta eller spara cache.
    public static CacheKey getCacheKeyFromByteBuf(ByteBuf buf){
        String data = buf.toString(StandardCharsets.UTF_8);
        String[] arrOfStr = data.split("\n");
        String[] keyStr = arrOfStr[0].split("\s");

        try{
            CacheKey key = new CacheKey(keyStr[0].toLowerCase(), keyStr[1].toLowerCase());
            for (CacheKey k : inMemoryCache.keySet()) {
                if(key.equals(k) && k.updatedAt.isAfter(Instant.now().minusSeconds(86400))){
                    return k;
                }
            }
            return key;
        }catch (Exception e){
            return new CacheKey("ERROR", "WILLNOTSAVE");
        }
    }
}
