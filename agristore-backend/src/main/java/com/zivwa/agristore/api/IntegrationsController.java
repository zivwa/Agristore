package com.zivwa.agristore.api;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@ConditionalOnProperty(name="app.auth-mode", havingValue="firebase")
@RestController @RequestMapping("/api/v1")
public class IntegrationsController {
 // TODO: implement OrderService with Firestore transactional stock reservation, idempotent payment state machine.
 @PostMapping("/checkout/quote") public ResponseEntity<?> quote(@RequestBody(required=false) Map<String,Object> request){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Checkout not configured");}
 @PostMapping("/orders") public ResponseEntity<?> orders(@RequestBody(required=false) Map<String,Object> request){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Order creation not configured");}
 @GetMapping("/orders") public ResponseEntity<?> myOrders(){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Orders not configured");}
 @PostMapping("/payments/razorpay/order") public ResponseEntity<?> payment(@RequestBody(required=false) Map<String,Object> request){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Razorpay not configured");}
 @PostMapping("/payments/razorpay/verify") public ResponseEntity<?> verify(@RequestBody(required=false) Map<String,Object> request){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Payment verification not configured");}
 @GetMapping("/shipping/serviceability") public ResponseEntity<?> shipping(){throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Shiprocket not configured");}
}
