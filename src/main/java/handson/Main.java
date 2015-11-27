package handson;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CustomLineItemDraft;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddCustomLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.ChangeLineItemQuantity;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static java.util.Arrays.asList;

public class Main {

    private static Cart addCustomLineItem(final SphereClient client, final Cart cart, final ProductProjection productProjection, final ProductVariant productVariant) {
        final LocalizedString name = productProjection.getName();
        final MonetaryAmount monetaryAmount = MoneyImpl.ofCents(1234, EUR);//this can come from SAP
        final String slug = productVariant.getSku();
        final Reference<TaxCategory> taxCategory = productProjection.getTaxCategory();
        final int quantity = 1;

        //TODO
        //values.put("productId", productProjection.getId());
        //values.put("productSlug", productProjection.getSlug());
        //values.put("productSku", productVariant.getSku());

        final CustomLineItemDraft customLineItemDraft = CustomLineItemDraft.of(name, slug, monetaryAmount, taxCategory, quantity);
        return execute(client, CartUpdateCommand.of(cart, AddCustomLineItem.of(customLineItemDraft)));
    }



    private static void doSomething(final SphereClient client) {
        final Cart cart = createCart(client);
        final String sku = "book-sku";

        final PagedQueryResult<ProductProjection> queryResult = execute(client, ProductProjectionQuery.ofCurrent()
                .withPredicates(product -> product.allVariants().where(variant -> variant.sku().is(sku))));

        if (!queryResult.getResults().isEmpty()) {
            final ProductProjection productProjection = queryResult.getResults().get(0);
            final String productId = productProjection.getId();
            final Optional<ProductVariant> variantOptional = productProjection.findVariantBySky(sku);
            if (variantOptional.isPresent()) {

                final ProductVariant productVariant = variantOptional.get();
                final int quantity = 1;

                final Cart updatedCart = addToCart(client, cart, productId, productVariant, quantity);

                final Cart cartWithMore = addCustomLineItem(client, updatedCart, productProjection, productVariant);

                final String sapOrderNumber = RandomStringUtils.randomNumeric(12);


                final OrderFromCartDraft draft = OrderFromCartDraft.of(cartWithMore, sapOrderNumber, PaymentState.PENDING);
                final Order order = execute(client, OrderFromCartCreateCommand.of(draft));
            }
        }
    }

    private static Cart createCart(final SphereClient client) {
        final CartDraft cartDraft = CartDraft.of(EUR)
                .withCountry(CountryCode.DE);
        final CartCreateCommand cartCreateCommand = CartCreateCommand.of(cartDraft);
        return execute(client, cartCreateCommand);
    }

    private static Cart changeLineItemQuantity(final SphereClient client, final Cart cart) {
        final ChangeLineItemQuantity changeLineItemQuantity = ChangeLineItemQuantity.of(cart.getLineItems().get(0).getId(), 4);
        return execute(client, CartUpdateCommand.of(cart, changeLineItemQuantity));
    }

    private static Cart addToCart(final SphereClient client, final Cart cart, final String productId, final ProductVariant productVariant, final int quantity) {
        final AddLineItem addLineItem = AddLineItem.of(productId, productVariant.getId(), quantity);
        final SetShippingAddress setShippingAddress = SetShippingAddress.of(Address.of(CountryCode.DE));
        final List<UpdateAction<Cart>> updateActions = asList(addLineItem, setShippingAddress);
        return execute(client, CartUpdateCommand.of(cart, updateActions));
    }

    private static <T> T execute(final SphereClient client, final SphereRequest<T> sphereRequest) {
        return client.execute(sphereRequest).toCompletableFuture().join();
    }


    public static void main(String[] args) throws IOException {
        final Properties prop = loadCommercetoolsPlatformProperties();
        final String projectKey = prop.getProperty("projectKey");
        final String clientId = prop.getProperty("clientId");
        final String clientSecret = prop.getProperty("clientSecret");
        final SphereClientConfig clientConfig = SphereClientConfig.of(projectKey, clientId, clientSecret);
        try(final SphereClient client = SphereClientFactory.of().createClient(clientConfig)) {
            doSomething(client);
        }
    }



    private static Properties loadCommercetoolsPlatformProperties() throws IOException {
        final Properties prop = new Properties();
        prop.load(Main.class.getClassLoader().getResourceAsStream("dev.properties"));
        return prop;
    }
}
