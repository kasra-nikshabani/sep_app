package ir.sepahan.app.shop;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional
    public Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    public List<CartItem> viewCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    /**
     * چک موجودی این‌جا فقط برای تجربه‌ی کاربری است (جلوگیری از افزودن مقدار بیشتر از
     * موجودی فعلی به سبد) -- بررسی نهایی و اتمی واقعی در لحظه‌ی Checkout انجام می‌شود
     * (طبق ADR-0012)، چون موجودی بین افزودن به سبد و پرداخت می‌تواند تغییر کند.
     */
    @Transactional
    public CartItem addItem(UUID userId, UUID productVariantId, int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "تعداد باید بزرگ‌تر از صفر باشد");
        }
        ProductVariant variant = productVariantRepository.findByIdAndDeletedAtIsNull(productVariantId)
                .filter(ProductVariant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "کالا یافت نشد"));

        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId)
                .orElse(null);
        int newQuantity = (item != null ? item.getQuantity() : 0) + quantity;
        if (newQuantity > variant.getStockQuantity()) {
            throw new InsufficientStockException(variant.getSku());
        }

        if (item != null) {
            item.setQuantity(newQuantity);
            return cartItemRepository.save(item);
        }
        return cartItemRepository.save(new CartItem(cart, variant, quantity));
    }

    @Transactional
    public void updateItemQuantity(UUID userId, UUID cartItemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "این آیتم در سبد شما نیست"));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }
        if (quantity > item.getProductVariant().getStockQuantity()) {
            throw new InsufficientStockException(item.getProductVariant().getSku());
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(UUID userId, UUID cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "این آیتم در سبد شما نیست"));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(UUID cartId) {
        cartItemRepository.deleteByCartId(cartId);
    }
}
