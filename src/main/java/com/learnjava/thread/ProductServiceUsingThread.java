package com.learnjava.thread;

import com.learnjava.domain.Product;
import com.learnjava.domain.ProductInfo;
import com.learnjava.domain.Review;
import com.learnjava.service.ProductInfoService;
import com.learnjava.service.ReviewService;

import static com.learnjava.util.CommonUtil.stopWatch;
import static com.learnjava.util.LoggerUtil.log;

public class ProductServiceUsingThread {
    private ProductInfoService productInfoService;
    private ReviewService reviewService;

    public ProductServiceUsingThread(ProductInfoService productInfoService, ReviewService reviewService) {
        this.productInfoService = productInfoService;
        this.reviewService = reviewService;
    }

    public Product retrieveProductDetails(String productId) {
        stopWatch.start();

        ProductInfoRunnable productInfoRunnable = new ProductInfoRunnable(productId);
        Thread productInfoThread = new Thread(productInfoRunnable);
//        ProductInfo productInfo = productInfoService.retrieveProductInfo(productId); // blocking call


        ReviewRunnable reviewRunnable = new ReviewRunnable(productId);
        Thread reviewThread = new Thread(reviewRunnable);
//        Review review = reviewService.retrieveReviews(productId); // blocking call

        // start execution of thread
        productInfoThread.start();
        reviewThread.start();

        //wait till execution of threads completed
        try {
            productInfoThread.join();
            reviewThread.join();
        } catch (InterruptedException e) {
            log("Some error occurred while joining threads: " +  e);
            throw new RuntimeException(e);
        }

        ProductInfo productInfo = productInfoRunnable.getProductInfo();
        Review review = reviewRunnable.getReview();

        stopWatch.stop();
        // It takes almost half time as compared to ProductService (synchronous) because of execution of
        // threads parallely.
        log("Total Time Taken : "+ stopWatch.getTime());
        return new Product(productId, productInfo, review);
    }

    public static void main(String[] args) {

        ProductInfoService productInfoService = new ProductInfoService();
        ReviewService reviewService = new ReviewService();
        ProductServiceUsingThread productService = new ProductServiceUsingThread(productInfoService, reviewService);
        String productId = "ABC123";
        Product product = productService.retrieveProductDetails(productId);
        log("Product is " + product);

    }

    private class ProductInfoRunnable implements Runnable {
        private ProductInfo productInfo;
        private final String productId;

        ProductInfoRunnable(final String productId) {
            this.productId = productId;
        }

        @Override
        public void run() {
            this.productInfo = productInfoService.retrieveProductInfo(productId);
        }

        public ProductInfo getProductInfo() {
            return this.productInfo;
        }
    }

    private class ReviewRunnable implements Runnable {
        private Review review;
        private final String productId;
        public ReviewRunnable(String productId) {
            this.productId = productId;
        }

        @Override
        public void run() {
            this.review = reviewService.retrieveReviews(productId); // blocking call
        }

        public Review getReview() {
            return this.review;
        }
    }
}
