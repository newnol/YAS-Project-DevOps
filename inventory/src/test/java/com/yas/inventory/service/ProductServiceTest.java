package com.yas.inventory.service;

import static com.yas.inventory.util.SecurityContextUtils.setUpSecurityContext;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.inventory.config.ServiceUrlConfig;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductServiceTest {

    private RestClient restClient;

    private ServiceUrlConfig serviceUrlConfig;

    private ProductService productService;

    private RestClient.ResponseSpec responseSpec;

    private static final String PRODUCT_URL = "http://api.yas.local/product";

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        setUpSecurityContext("test");
        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
    }

    @Test
    void testGetProduct_whenNormalCase_returnProductInfoVm() {

        Long productId = 1L;

        final URI url = UriComponentsBuilder
            .fromHttpUrl(PRODUCT_URL)
            .path("/backoffice/products/" + productId)
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec
            = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ProductInfoVm productInfoVm = new ProductInfoVm(productId,
            "ProductName", "ProductSKU", true);
        when(responseSpec.body(ProductInfoVm.class))
            .thenReturn(productInfoVm);

        ProductInfoVm result = productService.getProduct(productId);

        assertNotNull(result);
        assertEquals(productId, result.id());
        assertEquals("ProductName", result.name());
    }

    @Test
    void testFilterProducts_whenNormalCase_returnListProductInfoVm() {
        String productName = "ProductName";
        String productSku = "ProductSKU";
        List<Long> productIds = List.of(1L, 2L);
        FilterExistInWhSelection selection = FilterExistInWhSelection.YES;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", productName);
        params.add("sku", productSku);
        params.add("selection", selection.name());
        params.add("productIds", productIds.stream().map(String::valueOf).collect(Collectors.joining(",")));

        final URI url = UriComponentsBuilder
            .fromHttpUrl(PRODUCT_URL)
            .path("/backoffice/products/for-warehouse")
            .queryParams(params)
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec
            = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        ResponseEntity responseEntity = mock(ResponseEntity.class);
        ProductInfoVm productInfoVm = new ProductInfoVm(1L, productName, productSku, true);
        when(responseSpec.toEntity(new ParameterizedTypeReference<List<ProductInfoVm>>() {}))
            .thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(List.of(productInfoVm));

        List<ProductInfoVm> result = productService.filterProducts(productName, productSku, productIds, selection);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(productName, result.getFirst().name());
    }

    @Test
    void testUpdateProductQuantity_whenNormalCase_shouldNoException() {

        List<ProductQuantityPostVm> productQuantityPostVms = List.of(new ProductQuantityPostVm(1L, 100L));

        final URI url = UriComponentsBuilder
            .fromHttpUrl(serviceUrlConfig.product())
            .path("/backoffice/products/update-quantity")
            .buildAndExpand()
            .toUri();

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(productQuantityPostVms)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        assertDoesNotThrow(() -> productService.updateProductQuantity(productQuantityPostVms));
    }

    @Test
    void testStockService_addProductIntoWarehouse_whenValid_shouldSaveAll() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        com.yas.inventory.viewmodel.product.ProductInfoVm productInfoVm =
            new com.yas.inventory.viewmodel.product.ProductInfoVm(1L, "Product", "SKU", true);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(mockedProductService.getProduct(1L)).thenReturn(productInfoVm);
        com.yas.inventory.model.Warehouse warehouse = new com.yas.inventory.model.Warehouse();
        warehouse.setId(1L);
        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));

        java.util.List<com.yas.inventory.viewmodel.stock.StockPostVm> postVms =
            java.util.List.of(new com.yas.inventory.viewmodel.stock.StockPostVm(1L, 1L));

        stockService.addProductIntoWarehouse(postVms);

        org.mockito.Mockito.verify(stockRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void testStockService_addProductIntoWarehouse_whenStockAlreadyExists_shouldThrowStockExistingException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(true);

        java.util.List<com.yas.inventory.viewmodel.stock.StockPostVm> postVms =
            java.util.List.of(new com.yas.inventory.viewmodel.stock.StockPostVm(1L, 1L));

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.StockExistingException.class,
            () -> stockService.addProductIntoWarehouse(postVms));
    }

    @Test
    void testStockService_addProductIntoWarehouse_whenProductNotFound_shouldThrowNotFoundException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(mockedProductService.getProduct(1L)).thenReturn(null);

        java.util.List<com.yas.inventory.viewmodel.stock.StockPostVm> postVms =
            java.util.List.of(new com.yas.inventory.viewmodel.stock.StockPostVm(1L, 1L));

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(postVms));
    }

    @Test
    void testStockService_addProductIntoWarehouse_whenWarehouseNotFound_shouldThrowNotFoundException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        when(stockRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(mockedProductService.getProduct(1L)).thenReturn(
            new com.yas.inventory.viewmodel.product.ProductInfoVm(1L, "Product", "SKU", true));
        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        java.util.List<com.yas.inventory.viewmodel.stock.StockPostVm> postVms =
            java.util.List.of(new com.yas.inventory.viewmodel.stock.StockPostVm(1L, 1L));

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(postVms));
    }

    @Test
    void testStockService_getStocksByWarehouseIdAndProductNameAndSku_shouldReturnStockVms() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        java.util.List<com.yas.inventory.viewmodel.product.ProductInfoVm> products =
            java.util.List.of(new com.yas.inventory.viewmodel.product.ProductInfoVm(1L, "Product", "SKU", true));

        when(warehouseService.getProductWarehouse(1L, "name", "sku",
            com.yas.inventory.model.enumeration.FilterExistInWhSelection.YES)).thenReturn(products);

        com.yas.inventory.model.Stock stock = com.yas.inventory.model.Stock.builder()
            .id(1L)
            .productId(1L)
            .quantity(10L)
            .reservedQuantity(0L)
            .build();

        when(stockRepository.findByWarehouseIdAndProductIdIn(1L, java.util.List.of(1L)))
            .thenReturn(java.util.List.of(stock));

        java.util.List<com.yas.inventory.viewmodel.stock.StockVm> result =
            stockService.getStocksByWarehouseIdAndProductNameAndSku(1L, "name", "sku");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().productId());
    }

    @Test
    void testStockService_updateProductQuantityInStock_whenNoProductQuantityPostVms_shouldNotCallUpdateProductQuantity() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        ProductService mockedProductService = mock(ProductService.class);
        com.yas.inventory.service.WarehouseService warehouseService =
            mock(com.yas.inventory.service.WarehouseService.class);
        com.yas.inventory.service.StockHistoryService stockHistoryService =
            mock(com.yas.inventory.service.StockHistoryService.class);

        com.yas.inventory.service.StockService stockService = new com.yas.inventory.service.StockService(
            warehouseRepository, stockRepository, mockedProductService, warehouseService, stockHistoryService);

        com.yas.inventory.model.Stock stock = com.yas.inventory.model.Stock.builder()
            .id(1L)
            .productId(1L)
            .quantity(10L)
            .reservedQuantity(0L)
            .build();
        java.util.List<com.yas.inventory.model.Stock> stocks = java.util.List.of(stock);

        com.yas.inventory.viewmodel.stock.StockQuantityVm stockQuantityVm =
            new com.yas.inventory.viewmodel.stock.StockQuantityVm(1L, 5L, "note");

        com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm requestBody =
            new com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm(java.util.List.of(stockQuantityVm));

        when(stockRepository.findAllById(java.util.List.of(1L))).thenReturn(stocks);

        stockService.updateProductQuantityInStock(requestBody);

        org.mockito.Mockito.verify(stockHistoryService).createStockHistories(stocks,
            java.util.List.of(stockQuantityVm));
    }
}
