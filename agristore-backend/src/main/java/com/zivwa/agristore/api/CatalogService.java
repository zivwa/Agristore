package com.zivwa.agristore.api;
import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static com.zivwa.agristore.api.Models.*;
@Service
public class CatalogService {
 private final ObjectProvider<Firestore> database;private final String mode;
 private final Map<String,Category> categories=new ConcurrentHashMap<>();private final Map<String,Product> products=new ConcurrentHashMap<>();
 public CatalogService(ObjectProvider<Firestore> database,@Value("${app.catalog-source:demo}") String mode){
  this.database=database;this.mode=mode;
  categories.put("pest-control",new Category("pest-control","Pest Control","pest-control",true,1,"Demonstration category for pest control"));
  categories.put("nutrients",new Category("nutrients","Nutrients","nutrients",true,2,"Demonstration category for nutrients"));
  categories.put("products",new Category("products","Products","products",true,3,"Demonstration category for products"));
  categories.put("crop-kits",new Category("crop-kits","Crop Kit","crop-kits",true,4,"Demonstration category for crop kit"));
  categories.put("pest-fungal-control",new Category("pest-fungal-control","Pest / Fungal Control","pest-fungal-control",true,5,"Demonstration category for pest / fungal control"));
  categories.put("organic-natural",new Category("organic-natural","Organic & Natural Farming","organic-natural",true,6,"Demonstration category for organic & natural farming"));
  products.put("demo-pest-trap",new Product("demo-pest-trap","DEMO-PC-01","Yellow Sticky Insect Trap Pack","A sample insect-monitoring trap pack for testing the catalogue. Confirm product specifications before sale.","Field Supplies",12900L,"INR",35,true,List.of("pest-control","pest-fungal-control"),List.of(),List.of(),List.of()));
  products.put("demo-scout-card",new Product("demo-scout-card","DEMO-PC-02","Crop Pest Scouting Cards","Demo reference cards used for mock pest-monitoring product browsing.","Field Supplies",17900L,"INR",21,true,List.of("pest-control"),List.of(),List.of(),List.of()));
  products.put("demo-sprayer",new Product("demo-sprayer","DEMO-PC-03","Handheld Garden Sprayer","Sample hand sprayer product listing. Actual capacity and compatibility must be verified.","Farm Tools",49900L,"INR",13,true,List.of("pest-control","products"),List.of(),List.of(),List.of()));
  products.put("demo-seaweed",new Product("demo-seaweed","DEMO-NT-01","Seaweed Extract Plant Nutrient","Demonstration seaweed extract listing inspired by the example product layout. No application instructions or efficacy claims are supplied.","Demo Botanicals",35000L,"INR",42,true,List.of("nutrients","organic-natural"),List.of(),List.of(),List.of()));
  products.put("demo-micronutrient",new Product("demo-micronutrient","DEMO-NT-02","Micronutrient Blend Sample","Illustrative micronutrient product. Composition and approvals have not been verified.","Demo Botanicals",44900L,"INR",18,true,List.of("nutrients"),List.of(),List.of(),List.of()));
  products.put("demo-compost",new Product("demo-compost","DEMO-NT-03","Organic Compost Sample Pack","Fictional compost pack for catalogue and cart testing.","Demo Soil Care",22900L,"INR",32,true,List.of("nutrients","organic-natural"),List.of(),List.of(),List.of()));
  products.put("demo-seeds",new Product("demo-seeds","DEMO-PR-01","Vegetable Seeds Sample Packet","Sample vegetable seed packet with fictional stock and price.","Demo Seeds",29900L,"INR",50,true,List.of("products"),List.of(),List.of(),List.of()));
  products.put("demo-gloves",new Product("demo-gloves","DEMO-PR-02","Garden Work Gloves","Demo garden gloves listing for product detail and cart tests.","Field Supplies",19900L,"INR",27,true,List.of("products"),List.of(),List.of(),List.of()));
  products.put("demo-watering",new Product("demo-watering","DEMO-PR-03","Watering Can Sample","Demo watering can for storefront testing.","Farm Tools",39900L,"INR",11,true,List.of("products","organic-natural"),List.of(),List.of(),List.of()));
  products.put("demo-rice-kit",new Product("demo-rice-kit","DEMO-CK-01","Paddy Crop Observation Kit","Fictional starter kit for recording crop observations. Components must be confirmed before sale.","Crop Demo Kits",69900L,"INR",14,true,List.of("crop-kits"),List.of(),List.of(),List.of()));
  products.put("demo-tomato-kit",new Product("demo-tomato-kit","DEMO-CK-02","Tomato Garden Starter Kit","Sample tomato starter kit for demo browsing and stock tracking.","Crop Demo Kits",54900L,"INR",24,true,List.of("crop-kits","products"),List.of(),List.of(),List.of()));
  products.put("demo-soil-kit",new Product("demo-soil-kit","DEMO-CK-03","Soil Sampling Accessories Kit","Illustrative soil-sampling accessories set without laboratory analysis.","Crop Demo Kits",32900L,"INR",16,true,List.of("crop-kits","organic-natural"),List.of(),List.of(),List.of()));
  products.put("demo-plant-check",new Product("demo-plant-check","DEMO-PF-01","Plant Health Inspection Lens","Sample handheld lens for visual inspection of plant leaves.","Field Supplies",25900L,"INR",20,true,List.of("pest-fungal-control","pest-control"),List.of(),List.of(),List.of()));
  products.put("demo-leaf-kit",new Product("demo-leaf-kit","DEMO-PF-02","Leaf Observation Notebook Kit","Fictional notebook and observation kit to support UI testing.","Field Supplies",18900L,"INR",33,true,List.of("pest-fungal-control","crop-kits"),List.of(),List.of(),List.of()));
  products.put("demo-clean-tool",new Product("demo-clean-tool","DEMO-PF-03","Plant Care Tool Cleaning Kit","Demo tool-care listing. Actual contents and instructions must be verified.","Garden Demo",27900L,"INR",17,true,List.of("pest-fungal-control"),List.of(),List.of(),List.of()));
  products.put("demo-cocopeat",new Product("demo-cocopeat","DEMO-OR-01","Cocopeat Growing Medium Block","Illustrative cocopeat block pack for browsing and cart tests.","Demo Soil Care",24900L,"INR",28,true,List.of("organic-natural","products"),List.of(),List.of(),List.of()));
  products.put("demo-mulch",new Product("demo-mulch","DEMO-OR-02","Natural Mulch Sample Bag","Fictional natural mulch sample bag.","Demo Soil Care",19900L,"INR",15,true,List.of("organic-natural"),List.of(),List.of(),List.of()));
  products.put("demo-seedling",new Product("demo-seedling","DEMO-OR-03","Seedling Starter Tray","Demo reusable seedling tray listing for testing.","Farm Tools",14900L,"INR",38,true,List.of("organic-natural","products"),List.of(),List.of(),List.of()));
  String[][] storefront = {
   {"seeds","Seeds","category-seeds.jpeg"},
   {"insecticides","Insecticides","category-insecticides.jpeg"},
   {"fungicides","Fungicides","category-fungicides.jpeg"},
   {"herbicides","Herbicides","category-herbicides.jpeg"},
   {"growth-promoters","Growth Promoters","organic-kit/bio-fertiliser.png"},
   {"nutrients","Nutrients","category-nutrients.jpeg"},
   {"npk","N.P.K","category-npk.jpeg"}
  };
  int order = 0;
  for (String[] row : storefront) {
   categories.put(row[0], new Category(row[0], row[1], row[0], true, order++, "Storefront category"));
   String id = "sample-" + row[0];
   products.put(id, new Product(id, "SAMPLE-" + row[0].toUpperCase(), "Sample " + row[1] + " Pack",
    "Demonstration catalogue item only. Price, stock and product details are fictional; not available for fulfilment.",
    "AgriStore Demo", 19900L, "INR", 25, true, List.of(row[0]), List.of("/images/" + row[2]), List.of(), List.of()));
  }
  String[] crops = {"cotton","chilli","rice","groundnut","sugarcane","wheat","maize","soybean","tomato"};
  for (String crop : crops) {
   String id = "sample-" + crop + "-kit";
   String image = List.of("cotton","rice","groundnut","sugarcane","wheat","maize").contains(crop) ? crop + ".jpg" : "crop-kit-feature.jpeg";
   products.put(id, new Product(id, "SAMPLE-KIT-" + crop.toUpperCase(), "Sample " + crop + " Crop Kit",
    "Demo crop observation kit for sowing, germination, vegetative, flowering, fruiting and harvest. Fictional price and stock; not a treatment recommendation.",
    "AgriStore Demo", 39900L, "INR", 15, true, List.of("crop-kits"), List.of("/images/" + image), List.of(crop), List.of()));
  }
  for (String pest : List.of("whitefly","bollworms","aphids","thrips")) {
   String id = "sample-" + pest;
   products.put(id, new Product(id, "SAMPLE-PEST-" + pest.toUpperCase(), "Sample " + pest + " Monitoring Kit",
    "Demonstration pest observation accessory. No pesticide efficacy or application instructions. Fictional price and stock.",
    "AgriStore Demo", 12900L, "INR", 20, true, List.of("pest-control","insecticides"), List.of("/images/home-pests/" + pest + ".png"), List.of(), List.of()));
  }
 }
 public synchronized void restoreSnapshot(List<Category> categoryRows,List<Product> productRows){
  if(firestoreMode())throw new IllegalStateException("Use demo catalogue source with firebase-commerce persistence");
  categories.clear();for(Category category:categoryRows)categories.put(category.id(),category);
  products.clear();for(Product product:productRows)products.put(product.id(),product);
 } public boolean firestoreMode(){return mode.equalsIgnoreCase("firestore");}
 private Firestore db(){Firestore f=database.getIfAvailable();if(f==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firebase/Firestore not configured");return f;}
 private <T> T waitFor(ApiFuture<T> result){try{return result.get(15,TimeUnit.SECONDS);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Firestore operation failed. Check server logs.",e);}}
 private String str(Map<String,Object> m,String key,String def){Object x=m.get(key);return x==null?def:String.valueOf(x);}
 private boolean bool(Map<String,Object> m,String key,boolean def){Object x=m.get(key);return x instanceof Boolean b?b:def;}
 private int integer(Map<String,Object> m,String key,int def){Object x=m.get(key);return x instanceof Number n?n.intValue():def;}
 private long lng(Map<String,Object> m,String key,long def){Object x=m.get(key);return x instanceof Number n?n.longValue():def;}
 private List<String> strings(Map<String,Object> m,String key){Object x=m.get(key);if(!(x instanceof List<?> l))return List.of();return l.stream().filter(v->v instanceof String).map(v->(String)v).toList();}
 private Category category(String id,Map<String,Object> m){return new Category(id,str(m,"name",id),str(m,"slug",id),bool(m,"active",true),integer(m,"sortOrder",0),str(m,"description",""));}
 private Product product(String id,Map<String,Object> m,int stock){return new Product(id,str(m,"sku",id),str(m,"name",id),str(m,"description",""),str(m,"brand",""),lng(m,"pricePaise",0),str(m,"currency","INR"),stock,bool(m,"active",true),strings(m,"categoryIds"),strings(m,"imageUrls"),strings(m,"cropTags"),strings(m,"nutrientTags"));}
 private int stock(String id){DocumentSnapshot s=waitFor(db().collection("inventory").document(id).get());if(!s.exists())return 0;Long a=s.getLong("onHand"),r=s.getLong("reserved");return (int)Math.max(0,(a==null?0:a)-(r==null?0:r));}
 public List<Category> categories(boolean admin){
  List<Category> out=new ArrayList<>();if(firestoreMode()){for(DocumentSnapshot d:waitFor(db().collection("categories").get()).getDocuments())out.add(category(d.getId(),d.getData()==null?Map.of():d.getData()));}else out.addAll(categories.values());
  return out.stream().filter(c->admin||c.active()).sorted(Comparator.comparingInt(Category::sortOrder)).toList();
 }
 public List<Product> products(boolean admin){
  List<Product> out=new ArrayList<>();if(firestoreMode()){for(DocumentSnapshot d:waitFor(db().collection("products").get()).getDocuments())out.add(product(d.getId(),d.getData()==null?Map.of():d.getData(),stock(d.getId())));}else out.addAll(products.values());
  return out.stream().filter(p->admin||p.active()).sorted(Comparator.comparing(Product::name)).toList();
 }
 public Product detail(String id,boolean admin){return products(admin).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));}
 public ProductPage page(String query,String categoryId,String cursor,int limit){
  if(limit<1||limit>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit must be 1 to 50");
  List<Product> matched=products(false).stream().filter(p->categoryId==null||categoryId.isBlank()||p.categoryIds().contains(categoryId)).filter(p->query==null||query.isBlank()||(p.name()+" "+p.sku()+" "+p.description()+" "+p.brand()+" "+String.join(" ",p.categoryIds())+" "+String.join(" ",p.cropTags())+" "+String.join(" ",p.nutrientTags())).toLowerCase().contains(query.toLowerCase())).toList();
  int offset=0;try{if(cursor!=null&&!cursor.isBlank())offset=Integer.parseInt(cursor);}catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid cursor");}
  if(offset<0||offset>matched.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid cursor");int end=Math.min(matched.size(),offset+limit);
  return new ProductPage(matched.subList(offset,end),end<matched.size()?String.valueOf(end):null);
 }
 private Map<String,Object> categoryData(Category c){Map<String,Object> m=new HashMap<>();m.put("name",c.name());m.put("slug",c.slug());m.put("active",c.active());m.put("sortOrder",c.sortOrder());m.put("description",c.description());return m;}
 private Map<String,Object> productData(Product p){Map<String,Object> m=new HashMap<>();m.put("sku",p.sku());m.put("name",p.name());m.put("description",p.description());m.put("brand",p.brand());m.put("pricePaise",p.pricePaise());m.put("currency","INR");m.put("active",p.active());m.put("categoryIds",p.categoryIds());m.put("imageUrls",p.imageUrls());m.put("cropTags",p.cropTags());m.put("nutrientTags",p.nutrientTags());return m;}
 public Category saveCategory(String id,Category value){String key=id==null||id.isBlank()?value.slug():id;if(!key.matches("[a-z0-9-]{2,80}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid category slug");Category c=new Category(key,value.name(),key,value.active(),value.sortOrder(),value.description());if(firestoreMode())waitFor(db().collection("categories").document(key).set(categoryData(c)));else categories.put(key,c);return c;}
 public void deleteCategory(String id){boolean used=products(true).stream().anyMatch(p->p.categoryIds().contains(id));if(used)throw new ResponseStatusException(HttpStatus.CONFLICT,"Category is assigned to products");if(firestoreMode())waitFor(db().collection("categories").document(id).delete());else categories.remove(id);}
 public Product saveProduct(String id,Product p){
  if(p.name()==null||p.name().isBlank()||p.sku()==null||p.sku().isBlank()||p.pricePaise()<0)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Name, SKU and nonnegative price are required");
  if(p.categoryIds()==null||p.imageUrls()==null)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Category and images arrays required");
  for(String c:p.categoryIds())if(categories(true).stream().noneMatch(v->v.id().equals(c)))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Unknown category "+c);
  for(String url:p.imageUrls())if(!url.startsWith("https://") && !( !firestoreMode() && (url.startsWith("/demo-products/") || url.startsWith("/images/")) ))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Image URLs must use HTTPS");
  String key=id==null||id.isBlank()?UUID.randomUUID().toString():id;int current=0;try{current=detail(key,true).availableStock();}catch(ResponseStatusException e){if(e.getStatusCode().value()!=404)throw e;}
  Product saved=new Product(key,p.sku(),p.name(),p.description(),p.brand(),p.pricePaise(),"INR",current,p.active(),p.categoryIds(),p.imageUrls(),p.cropTags()==null?List.of():p.cropTags(),p.nutrientTags()==null?List.of():p.nutrientTags());
  if(firestoreMode())waitFor(db().collection("products").document(key).set(productData(saved)));else products.put(key,saved);return saved;
 }
 public void deactivate(String id){Product p=detail(id,true);saveProduct(id,new Product(id,p.sku(),p.name(),p.description(),p.brand(),p.pricePaise(),p.currency(),p.availableStock(),false,p.categoryIds(),p.imageUrls(),p.cropTags(),p.nutrientTags()));}
 public Product updateStock(String id,int amount){if(amount<0)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Stock cannot be negative");Product p=detail(id,true);if(firestoreMode()){
  DocumentReference ref=db().collection("inventory").document(id);waitFor(db().runTransaction(tx->{DocumentSnapshot d=tx.get(ref).get();long reserved=d.exists()&&d.getLong("reserved")!=null?d.getLong("reserved"):0;tx.set(ref,Map.of("onHand",(long)amount+reserved,"reserved",reserved));return true;}));
  return detail(id,true);
  }Product updated=new Product(p.id(),p.sku(),p.name(),p.description(),p.brand(),p.pricePaise(),p.currency(),amount,p.active(),p.categoryIds(),p.imageUrls(),p.cropTags(),p.nutrientTags());products.put(id,updated);return updated;
 }
}
