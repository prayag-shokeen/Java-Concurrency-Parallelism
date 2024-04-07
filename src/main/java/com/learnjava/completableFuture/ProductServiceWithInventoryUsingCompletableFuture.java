package com.learnjava.completableFuture;

import com.learnjava.domain.*;
import com.learnjava.service.InventoryService;
import com.learnjava.service.ProductInfoService;
import com.learnjava.service.ReviewService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.learnjava.util.CommonUtil.*;
import static com.learnjava.util.LoggerUtil.log;

public class ProductServiceWithInventoryUsingCompletableFuture {
    private ProductInfoService productInfoService;
    private ReviewService reviewService;
    private InventoryService inventoryService;

    public ProductServiceWithInventoryUsingCompletableFuture(ProductInfoService productInfoService, ReviewService reviewService, InventoryService inventoryService) {
        this.productInfoService = productInfoService;
        this.reviewService = reviewService;
        this.inventoryService = inventoryService;
    }

    public Product retrieveProductDetailsCompletableFuture(String productId) {
        startTimer();

        CompletableFuture<ProductInfo> productInfoCompletableFuture = CompletableFuture.supplyAsync(() -> productInfoService.retrieveProductInfo(productId))
                .thenApply(productInfo -> {
                    productInfo.setProductOptions(inventoryUpdatedProductOptionsWithCompletableFuture(productInfo));
                    return productInfo;
                });

        CompletableFuture<Review> reviewCompletableFuture = CompletableFuture.supplyAsync(() -> reviewService.retrieveReviews(productId));

        CompletableFuture<Product> productCompletableFuture = productInfoCompletableFuture.thenCombine(
                reviewCompletableFuture,
                (productInfoResponse, reviewResponse) -> new Product(productId, productInfoResponse, reviewResponse));

        final Product product = productCompletableFuture.join();
        timeTaken();
        return product;
    }

    // below method is not efficient because it is calling inventoryService (latency 500ms) for each
    // productOption synchronously.
    // Latency: 4000 ms (approx)
    private List<ProductOption> inventoryUpdatedProductOptions(final ProductInfo productInfo) {
        return productInfo.getProductOptions().stream()
                .map(productOption -> {
                    Inventory inventory = inventoryService.addInventory(productOption);
                    productOption.setInventory(inventory);
                    return productOption;
                }).collect(Collectors.toList());
    }

    // async calls to inventoryService using CompletableFuture in stream.
    // Latency: 1500 ms (approx)
    private List<ProductOption> inventoryUpdatedProductOptionsWithCompletableFuture(final ProductInfo productInfo) {
        List<CompletableFuture<ProductOption>> productOptionFutureList =  productInfo.getProductOptions().stream()
                .map(productOption -> {
                    CompletableFuture<ProductOption> productOptionFuture = CompletableFuture
                            .supplyAsync(() -> inventoryService.addInventory(productOption))
                            .thenApply((inventory -> {
                                productOption.setInventory(inventory);
                                return productOption;
                            }));
                    return productOptionFuture;
                }).collect(Collectors.toList());

        return productOptionFutureList.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    // if we have multiple completable futures to join, then we can use allOf to further improve latency
    // as above method inventoryUpdatedProductOptionsWithCompletableFuture() has a list of CompletableFuture
    // we can use allOf here.
    // similarly we have anyOf also, which we can use if we want to return if any completable future gives response first.
    // So the use case for anyOf can be if we can retrieve data from multiple dataSources like
    //          1. DB
    //          2. S3
    //          3. API call
    // and all dataSources returns the same result for given request, then we can use anyOf() to make
    // our code fast whichever service returns the response first we will use the response and done.
    private List<ProductOption> inventoryUpdatedProductOptionsWithCompletableFuture_AllOf(final ProductInfo productInfo) {
        List<CompletableFuture<ProductOption>> productOptionFutureList =  productInfo.getProductOptions().stream()
                .map(productOption -> {
                    CompletableFuture<ProductOption> productOptionFuture = CompletableFuture
                            .supplyAsync(() -> inventoryService.addInventory(productOption))
                            .thenApply((inventory -> {
                                productOption.setInventory(inventory);
                                return productOption;
                            }));
                    return productOptionFuture;
                }).collect(Collectors.toList());

        var cfAllOf = CompletableFuture.allOf(productOptionFutureList.toArray(new CompletableFuture[productOptionFutureList.size()]));

        return cfAllOf
                .thenApply((v) -> productOptionFutureList.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .join();
    }

    // async call using parallelStreams
    // Latency: 1500 ms (approx)
    private List<ProductOption> inventoryUpdatedProductOptionsWithParallelStream(final ProductInfo productInfo) {
        return productInfo.getProductOptions().parallelStream()
                .map(productOption -> {
                    Inventory inventory = inventoryService.addInventory(productOption);
                    productOption.setInventory(inventory);
                    return productOption;
                }).collect(Collectors.toList());
    }

    public static void main(String[] args) {

        ProductInfoService productInfoService = new ProductInfoService();
        ReviewService reviewService = new ReviewService();
        InventoryService inventoryService = new InventoryService();
        ProductServiceWithInventoryUsingCompletableFuture productService = new ProductServiceWithInventoryUsingCompletableFuture(productInfoService, reviewService, inventoryService);
        String productId = "ABC123";
        Product product = productService.retrieveProductDetailsCompletableFuture(productId);
        log("Product is " + product);

    }
}
