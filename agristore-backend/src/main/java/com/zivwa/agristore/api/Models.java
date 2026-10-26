package com.zivwa.agristore.api;
import java.util.*;
public class Models {
 public record Category(String id,String name,String slug,boolean active,int sortOrder,String description){}
 public record Product(String id,String sku,String name,String description,String brand,long pricePaise,String currency,int availableStock,boolean active,List<String> categoryIds,List<String> imageUrls,List<String> cropTags,List<String> nutrientTags){}
 public record ProductPage(List<Product> items,String nextCursor){}
 public record CartItem(String productId,int quantity,String name,long unitPricePaise,long lineTotalPaise,int availableStock,boolean active,String imageUrl){}
 public record Cart(List<CartItem> items,long itemsSubtotalPaise,String currency){}
 public record Address(String id,String recipientName,String phone,String line1,String line2,String city,String stateCode,String pincode,String countryCode){}
 public record AddressInput(String recipientName,String phone,String line1,String line2,String city,String stateCode,String pincode,String countryCode){}
}
