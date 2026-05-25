package com.yas.inventory.service;

import static com.yas.inventory.util.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.inventory.config.ServiceUrlConfig;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class LocationServiceTest {
    private RestClient restClient;

    private ServiceUrlConfig serviceUrlConfig;

    private LocationService locationService;

    private RestClient.ResponseSpec responseSpec;

    private static final String INVENTORY_URL = "http://api.yas.local/inventory";

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        locationService = new LocationService(restClient, serviceUrlConfig);
        responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        setUpSecurityContext("test");
        when(serviceUrlConfig.location()).thenReturn(INVENTORY_URL);
    }

    @Test
    void testGetAddressById_ifNormalCase_returnAddressDetailVm() {

        Long addressId = 1L;
        AddressDetailVm addressDetail = AddressDetailVm.builder()
            .id(1L)
            .contactName("John Doe")
            .phone("123-456-7890")
            .addressLine1("123 Main St")
            .addressLine2("Apt 4B")
            .city("Metropolis")
            .zipCode("12345")
            .districtId(100L)
            .districtName("Central District")
            .stateOrProvinceId(200L)
            .stateOrProvinceName("StateName")
            .countryId(300L)
            .countryName("CountryName")
            .build();

        final URI url = UriComponentsBuilder
            .fromHttpUrl(serviceUrlConfig.location())
            .path("/storefront/addresses/{id}")
            .buildAndExpand(addressId)
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AddressDetailVm.class))
            .thenReturn(addressDetail);

        AddressDetailVm result = locationService.getAddressById(addressId);

        assertNotNull(result);
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void testCreateAddress_ifNormalCase_returnAddressVm() {
        AddressPostVm addressPost = new AddressPostVm(
            "John Smith",
            "555-1234",
            "789 Oak St",
            "Apt 101",
            "Smalltown",
            "67890",
            1L,
            2L,
            3L
        );
        AddressVm address = AddressVm.builder()
            .id(1L)
            .contactName("Jane Doe")
            .phone("987-654-3210")
            .addressLine1("456 Elm St")
            .city("Gotham")
            .zipCode("54321")
            .districtId(10L)
            .stateOrProvinceId(20L)
            .countryId(30L)
            .build();

        final URI url = UriComponentsBuilder
            .fromHttpUrl(serviceUrlConfig.location())
            .path("/storefront/addresses")
            .buildAndExpand()
            .toUri();

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(addressPost)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AddressVm.class)).thenReturn(address);

        AddressVm result = locationService.createAddress(addressPost);

        assertNotNull(result);
        assertThat(result.id()).isEqualTo(1L);
    }


    @Test
    void testUpdateAddress_ifNormalCase_shouldNoException() {
        Long addressId = 1L;
        AddressPostVm addressPostVm = new AddressPostVm(
            "Alice Johnson",
            "555-9876",
            "123 Maple Ave",
            "Suite 2",
            "Springfield",
            "98765",
            100L,
            200L,
            300L
        );

        final URI url = UriComponentsBuilder
            .fromHttpUrl(serviceUrlConfig.location())
            .path("/storefront/addresses/{id}")
            .buildAndExpand(addressId)
            .toUri();

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(addressPostVm)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        assertDoesNotThrow(() -> locationService.updateAddress(addressId, addressPostVm));
    }

    @Test
    void testDeleteAddress_ifNormalCase_shouldNoException() {

        Long addressId = 1L;
        final URI url = UriComponentsBuilder.fromHttpUrl(serviceUrlConfig.location()).path("/storefront/addresses/{id}")
            .buildAndExpand(addressId).toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        assertDoesNotThrow(() -> locationService.deleteAddress(addressId));
    }

    @Test
    void testWarehouseService_findAllWarehouses_shouldReturnList() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = com.yas.inventory.model.Warehouse.builder()
            .id(1L)
            .name("Warehouse1")
            .addressId(1L)
            .build();

        when(warehouseRepository.findAll()).thenReturn(java.util.List.of(warehouse));

        java.util.List<com.yas.inventory.viewmodel.warehouse.WarehouseGetVm> result =
            warehouseService.findAllWarehouses();

        assertNotNull(result);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Warehouse1");
    }

    @Test
    void testWarehouseService_findById_shouldReturnDetailVm() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = com.yas.inventory.model.Warehouse.builder()
            .id(1L)
            .name("Warehouse1")
            .addressId(10L)
            .build();

        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));

        AddressDetailVm addressDetailVm = AddressDetailVm.builder()
            .id(10L)
            .contactName("John")
            .phone("123")
            .addressLine1("Street")
            .addressLine2("Line2")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        when(mockedLocationService.getAddressById(10L)).thenReturn(addressDetailVm);

        com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm result = warehouseService.findById(1L);

        assertNotNull(result);
        assertThat(result.name()).isEqualTo("Warehouse1");
        assertThat(result.contactName()).isEqualTo("John");
    }

    @Test
    void testWarehouseService_findById_whenNotFound_shouldThrowNotFoundException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.NotFoundException.class,
            () -> warehouseService.findById(1L));
    }

    @Test
    void testWarehouseService_create_whenNameExists_shouldThrowDuplicatedException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.viewmodel.warehouse.WarehousePostVm postVm =
            com.yas.inventory.viewmodel.warehouse.WarehousePostVm.builder()
                .name("Warehouse1")
                .contactName("John")
                .phone("123")
                .addressLine1("Street")
                .city("City")
                .zipCode("00000")
                .districtId(1L)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();

        when(warehouseRepository.existsByName("Warehouse1")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.DuplicatedException.class,
            () -> warehouseService.create(postVm));
    }

    @Test
    void testWarehouseService_create_whenValid_shouldSaveWarehouse() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.viewmodel.warehouse.WarehousePostVm postVm =
            com.yas.inventory.viewmodel.warehouse.WarehousePostVm.builder()
                .name("Warehouse1")
                .contactName("John")
                .phone("123")
                .addressLine1("Street")
                .city("City")
                .zipCode("00000")
                .districtId(1L)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();

        when(warehouseRepository.existsByName("Warehouse1")).thenReturn(false);
        AddressVm addressVm = AddressVm.builder()
            .id(10L)
            .contactName("John")
            .phone("123")
            .addressLine1("Street")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
        when(mockedLocationService.createAddress(org.mockito.ArgumentMatchers.any(AddressPostVm.class)))
            .thenReturn(addressVm);

        com.yas.inventory.model.Warehouse saved = new com.yas.inventory.model.Warehouse();
        saved.setId(1L);
        saved.setName("Warehouse1");
        saved.setAddressId(10L);
        when(warehouseRepository.save(org.mockito.ArgumentMatchers.any(com.yas.inventory.model.Warehouse.class)))
            .thenReturn(saved);

        com.yas.inventory.model.Warehouse result = warehouseService.create(postVm);

        assertNotNull(result);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAddressId()).isEqualTo(10L);
    }

    @Test
    void testWarehouseService_update_whenNameDuplicated_shouldThrowDuplicatedException() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = new com.yas.inventory.model.Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse1");
        warehouse.setAddressId(10L);

        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("NewName", 1L)).thenReturn(true);

        com.yas.inventory.viewmodel.warehouse.WarehousePostVm postVm =
            com.yas.inventory.viewmodel.warehouse.WarehousePostVm.builder()
                .name("NewName")
                .contactName("John")
                .phone("123")
                .addressLine1("Street")
                .city("City")
                .zipCode("00000")
                .districtId(1L)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
            com.yas.commonlibrary.exception.DuplicatedException.class,
            () -> warehouseService.update(postVm, 1L));
    }

    @Test
    void testWarehouseService_update_whenValid_shouldSaveAndUpdateAddress() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = new com.yas.inventory.model.Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse1");
        warehouse.setAddressId(10L);

        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("NewName", 1L)).thenReturn(false);

        com.yas.inventory.viewmodel.warehouse.WarehousePostVm postVm =
            com.yas.inventory.viewmodel.warehouse.WarehousePostVm.builder()
                .name("NewName")
                .contactName("John")
                .phone("123")
                .addressLine1("Street")
                .city("City")
                .zipCode("00000")
                .districtId(1L)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();

        warehouseService.update(postVm, 1L);

        org.mockito.Mockito.verify(warehouseRepository).save(warehouse);
        org.mockito.Mockito.verify(mockedLocationService)
            .updateAddress(org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any(AddressPostVm.class));
    }

    @Test
    void testWarehouseService_delete_whenValid_shouldDeleteAndRemoveAddress() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = new com.yas.inventory.model.Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse1");
        warehouse.setAddressId(10L);

        when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(warehouse));

        warehouseService.delete(1L);

        org.mockito.Mockito.verify(warehouseRepository).deleteById(1L);
        org.mockito.Mockito.verify(mockedLocationService).deleteAddress(10L);
    }

    @Test
    void testWarehouseService_getPageableWarehouses_shouldReturnPagedResult() {
        com.yas.inventory.repository.WarehouseRepository warehouseRepository =
            mock(com.yas.inventory.repository.WarehouseRepository.class);
        com.yas.inventory.repository.StockRepository stockRepository =
            mock(com.yas.inventory.repository.StockRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);
        LocationService mockedLocationService = mock(LocationService.class);

        com.yas.inventory.service.WarehouseService warehouseService =
            new com.yas.inventory.service.WarehouseService(warehouseRepository, stockRepository, productService,
                mockedLocationService);

        com.yas.inventory.model.Warehouse warehouse = new com.yas.inventory.model.Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse1");
        warehouse.setAddressId(10L);

        org.springframework.data.domain.Page<com.yas.inventory.model.Warehouse> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(warehouse));

        when(warehouseRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 10)))
            .thenReturn(page);

        com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm result =
            warehouseService.getPageableWarehouses(0, 10);

        assertNotNull(result);
        assertThat(result.warehouseContent()).hasSize(1);
        assertThat(result.warehouseContent().getFirst().name()).isEqualTo("Warehouse1");
    }

    @Test
    void testStockHistoryService_createStockHistories_shouldSaveHistoryForMatchingStocks() {
        com.yas.inventory.repository.StockHistoryRepository stockHistoryRepository =
            mock(com.yas.inventory.repository.StockHistoryRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);

        com.yas.inventory.service.StockHistoryService stockHistoryService =
            new com.yas.inventory.service.StockHistoryService(stockHistoryRepository, productService);

        com.yas.inventory.model.Stock stock = com.yas.inventory.model.Stock.builder()
            .id(1L)
            .productId(1L)
            .quantity(10L)
            .reservedQuantity(0L)
            .build();

        com.yas.inventory.viewmodel.stock.StockQuantityVm stockQuantityVm =
            new com.yas.inventory.viewmodel.stock.StockQuantityVm(1L, 5L, "note");

        java.util.List<com.yas.inventory.model.Stock> stocks = java.util.List.of(stock);
        java.util.List<com.yas.inventory.viewmodel.stock.StockQuantityVm> stockQuantityVms =
            java.util.List.of(stockQuantityVm);

        stockHistoryService.createStockHistories(stocks, stockQuantityVms);

        org.mockito.Mockito.verify(stockHistoryRepository)
            .saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void testStockHistoryService_getStockHistories_shouldReturnListVm() {
        com.yas.inventory.repository.StockHistoryRepository stockHistoryRepository =
            mock(com.yas.inventory.repository.StockHistoryRepository.class);
        com.yas.inventory.service.ProductService productService =
            mock(com.yas.inventory.service.ProductService.class);

        com.yas.inventory.service.StockHistoryService stockHistoryService =
            new com.yas.inventory.service.StockHistoryService(stockHistoryRepository, productService);

        com.yas.inventory.model.StockHistory history = com.yas.inventory.model.StockHistory.builder()
            .id(1L)
            .productId(1L)
            .adjustedQuantity(5L)
            .note("note")
            .build();

        java.util.List<com.yas.inventory.model.StockHistory> histories =
            java.util.List.of(history);

        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(1L, 2L))
            .thenReturn(histories);

        com.yas.inventory.viewmodel.product.ProductInfoVm productInfoVm =
            new com.yas.inventory.viewmodel.product.ProductInfoVm(1L, "Product", "SKU", true);
        when(productService.getProduct(1L)).thenReturn(productInfoVm);

        com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm result =
            stockHistoryService.getStockHistories(1L, 2L);

        assertNotNull(result);
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().getFirst().productName()).isEqualTo("Product");
    }
} // nothing